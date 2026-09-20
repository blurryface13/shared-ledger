package com.spvermicelli.tripledger.export.infrastructure.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExportOutbox {
    private final JdbcTemplate jdbc;
    public ExportOutbox(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Long exportRecordId) {
        jdbc.update("INSERT INTO export_outbox(export_record_id) VALUES (?)", exportRecordId);
    }
}
