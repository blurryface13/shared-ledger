package com.spvermicelli.tripledger.export.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** One bounded publish per transaction. SKIP LOCKED lets other instances make progress. */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.export.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class ExportOutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(ExportOutboxRelay.class);
    private final JdbcTemplate jdbc;
    private final ExportTaskPublisher publisher;
    private final TransactionTemplate transaction;
    public ExportOutboxRelay(JdbcTemplate jdbc, ExportTaskPublisher publisher, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.publisher = publisher;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setTimeout(10);
    }

    @Scheduled(fixedDelayString = "${app.export.outbox.poll-ms:1000}")
    public void poll() {
        try { relayOne(); }
        catch (RuntimeException e) { log.error("Export outbox scan failed; record remains recoverable", e); }
    }

    public boolean relayOne() {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            var rows = jdbc.queryForList("""
                SELECT export_record_id, attempts FROM export_outbox
                WHERE status='PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP(6)
                ORDER BY next_attempt_at, export_record_id LIMIT 1 FOR UPDATE SKIP LOCKED
                """);
            if (rows.isEmpty()) return false;
            var row = rows.getFirst();
            long id = ((Number) row.get("export_record_id")).longValue();
            int attempts = ((Number) row.get("attempts")).intValue() + 1;
            try {
                publisher.publish(id);
            } catch (RuntimeException e) {
                String state = attempts >= 8 ? "DEAD" : "PENDING";
                int delay = Math.min(300, 1 << attempts);
                jdbc.update("""
                    UPDATE export_outbox SET status=?, attempts=?, last_error=?,
                    next_attempt_at=TIMESTAMPADD(SECOND, ?, CURRENT_TIMESTAMP(6))
                    WHERE export_record_id=?
                    """, state, attempts, e.getClass().getSimpleName(), delay, id);
                log.warn("Export outbox publish failed id={} attempt={} state={}", id, attempts, state);
                return true;
            }
            // If this write/commit fails after broker acknowledgement, a later scan republishes.
            // The consumer must remain idempotent; this is at-least-once delivery.
            jdbc.update("""
                UPDATE export_outbox SET status='SENT', attempts=?, last_error=NULL,
                sent_at=CURRENT_TIMESTAMP(6) WHERE export_record_id=?
                """, attempts, id);
            return true;
        }));
    }
}
