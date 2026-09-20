package com.spvermicelli.tripledger.export.infrastructure.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExportOutbox {
    private final JdbcTemplate jdbc;
    public ExportOutbox(JdbcTemplate jdbc) { this.jdbc = jdbc; }

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
