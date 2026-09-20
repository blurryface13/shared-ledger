package com.spvermicelli.tripledger.export.infrastructure.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExportOutbox {
    private final JdbcTemplate jdbc;
    public ExportOutbox(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Delivery(String status, int attempts, java.time.LocalDateTime nextAttemptAt) {}

    /** Read only previously authorized IDs, in bounded batches rather than one query per task. */
    public java.util.Map<Long, Delivery> findDeliveries(java.util.List<Long> ids) {
        var result = new java.util.HashMap<Long, Delivery>();
        for (int start = 0; start < ids.size(); start += 200) {
            var batch = ids.subList(start, Math.min(start + 200, ids.size()));
            String placeholders = String.join(",", java.util.Collections.nCopies(batch.size(), "?"));
            jdbc.query("SELECT export_record_id,status,attempts,next_attempt_at FROM export_outbox WHERE export_record_id IN ("
                + placeholders + ")", rs -> {
                    var next = rs.getTimestamp("next_attempt_at");
                    result.put(rs.getLong("export_record_id"), new Delivery(rs.getString("status"),
                        rs.getInt("attempts"), next == null ? null : next.toLocalDateTime()));
                }, batch.toArray());
        }
        return result;
    }

    /** Caller holds the export record lock; reset only terminal delivery states. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean retry(Long exportRecordId, boolean processingFailed) {
        var states = jdbc.queryForList("SELECT status FROM export_outbox WHERE export_record_id=? FOR UPDATE",
            String.class, exportRecordId);
        if (states.isEmpty()) {
            if (!processingFailed) return false;
            enqueue(exportRecordId); // Legacy failed task created before Outbox.
            return true;
        }
        String state = states.getFirst();
        if (!"DEAD".equals(state) && !(processingFailed && "SENT".equals(state))) return false;
        jdbc.update("""
            UPDATE export_outbox SET status='PENDING', attempts=0, next_attempt_at=CURRENT_TIMESTAMP(6),
              sent_at=NULL, last_error=NULL WHERE export_record_id=?
            """, exportRecordId);
        return true;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Long exportRecordId) {
        jdbc.update("INSERT INTO export_outbox(export_record_id) VALUES (?)", exportRecordId);
    }
}
