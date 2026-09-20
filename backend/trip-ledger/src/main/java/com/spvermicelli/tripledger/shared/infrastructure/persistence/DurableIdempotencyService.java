package com.spvermicelli.tripledger.shared.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.TreeSet;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** The result and business writes commit together on the same datasource. */
@Service
public class DurableIdempotencyService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;

    public DurableIdempotencyService(JdbcTemplate jdbc, ObjectMapper json, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.json = json;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setTimeout(15);
    }

    public <T> T execute(Long userId, Long bookId, String operation, String key,
                         Object request, Class<T> responseType, Runnable authorize, Supplier<T> action) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{16,128}")) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "Idempotency-Key 必须为 16–128 位字母、数字、下划线或连字符");
        }
        String digest = digest(request);
        return transaction.execute(status -> {
            authorize.run(); // Replayed results still require current access.
            // The unique primary key serializes concurrent owners across all instances.
            jdbc.update("""
                INSERT INTO request_idempotency(user_id, book_id, operation, request_key, request_hash)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE request_key = request_idempotency.request_key
                """, userId, bookId, operation, key, digest);
            var row = jdbc.queryForMap("""
                SELECT request_hash, response_json FROM request_idempotency
                WHERE user_id=? AND book_id=? AND operation=? AND request_key=? FOR UPDATE
                """, userId, bookId, operation, key);
            if (!digest.equals(row.get("request_hash"))) {
                throw new BusinessException(ErrorCode.CONFLICT, "同一个请求号不能用于不同的账单内容");
            }
            if (row.get("response_json") != null) {
                try { return json.readValue((String) row.get("response_json"), responseType); }
                catch (Exception e) { throw new IllegalStateException("Cannot replay stored operation", e); }
            }
            T response = action.get();
            try {
                jdbc.update("""
                    UPDATE request_idempotency SET response_json=?
                    WHERE user_id=? AND book_id=? AND operation=? AND request_key=?
                    """, json.writeValueAsString(response), userId, bookId, operation, key);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalStateException("Cannot persist operation result", e);
            }
            return response;
        });
    }

    private String digest(Object request) {
        try {
            byte[] bytes = json.writeValueAsString(canonical(json.valueToTree(request))).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) { throw new IllegalArgumentException("Cannot fingerprint request", e); }
    }

    private JsonNode canonical(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = json.createObjectNode();
            var fields = new TreeSet<String>();
            node.fieldNames().forEachRemaining(fields::add);
            fields.forEach(name -> result.set(name, canonical(node.get(name))));
            return result;
        }
        if (node.isArray()) {
            var result = json.createArrayNode();
            node.forEach(item -> result.add(canonical(item)));
            return result;
        }
        return node;
    }
}
