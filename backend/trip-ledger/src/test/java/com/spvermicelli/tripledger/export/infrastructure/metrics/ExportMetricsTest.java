package com.spvermicelli.tripledger.export.infrastructure.metrics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;

class ExportMetricsTest {
    @Test void gaugesShareOneSampleAndExposeBusinessCounts() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForMap(anyString())).thenReturn(Map.of("pending", 2L, "dead", 1L,
            "failed", 3L, "oldest_pending_seconds", 301L));
        var registry = new SimpleMeterRegistry();
        var metrics = new ExportMetrics(jdbc);
        metrics.bindTo(registry);
        assertEquals(2, registry.get("tripledger.export.pending").gauge().value());
        assertEquals(1, registry.get("tripledger.export.dead").gauge().value());
        assertEquals(301, registry.get("tripledger.export.oldest_pending_seconds").gauge().value());
        assertEquals(1, registry.get("tripledger.export.collection_success").gauge().value());
        verify(jdbc, times(1)).queryForMap(anyString());
        java.lang.ref.Reference.reachabilityFence(metrics); // Gauge holds a weak reference; keep the test binder alive.
        registry.close();
    }
    @Test void collectionFailureIsNotReportedAsHealthyZero() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForMap(anyString())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"));
        var metrics = new ExportMetrics(jdbc);
        assertEquals(-1, metrics.read("pending"));
        assertEquals(0, metrics.read("collection_success"));
        verify(jdbc, times(1)).queryForMap(anyString());
    }
}
