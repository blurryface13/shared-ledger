package com.spvermicelli.tripledger.export.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

/** A bounded five-second sample shared by all gauges; no task/user IDs as metric tags. */
@Component
public class ExportMetrics implements MeterBinder {
    private final JdbcTemplate jdbc;
    private Map<String, Object> sample = Map.of();
    private long expiresAt;
    public ExportMetrics(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public void bindTo(MeterRegistry registry) {
        for (String metric : new String[]{"pending", "dead", "failed", "oldest_pending_seconds", "collection_success"}) {
            registry.gauge("tripledger.export." + metric, this, value -> value.read(metric));
        }
    }
    synchronized double read(String name) {
        if (System.nanoTime() >= expiresAt) {
            try {
                var values = new java.util.HashMap<>(jdbc.queryForMap("""
                    SELECT
                    (SELECT COUNT(*) FROM export_outbox WHERE status='PENDING') AS pending,
                    (SELECT COUNT(*) FROM export_outbox WHERE status='DEAD') AS dead,
                    (SELECT COUNT(*) FROM tb_export_record WHERE export_status='FAILED') AS failed,
                    (SELECT COALESCE(MAX(TIMESTAMPDIFF(SECOND,created_at,NOW())),0)
                     FROM export_outbox WHERE status='PENDING') AS oldest_pending_seconds
                    """));
                values.put("collection_success", 1);
                sample = values;
            } catch (RuntimeException failure) {
                // A failed query must not turn into a healthy zero or a stale successful sample.
                sample = Map.of("collection_success", 0);
            }
            expiresAt = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        }
        return ((Number) sample.getOrDefault(name, -1)).doubleValue();
    }
}
