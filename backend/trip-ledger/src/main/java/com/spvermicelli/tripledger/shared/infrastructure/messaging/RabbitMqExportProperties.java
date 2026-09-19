package com.spvermicelli.tripledger.shared.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitMqExportProperties {

    private String exportExchange = "trip-ledger.export.exchange";
    private String exportRoutingKey = "trip-ledger.export.created";
    private String exportQueue = "trip-ledger.export.queue";

    public String getExportExchange() {
        return exportExchange;
    }

    public void setExportExchange(String exportExchange) {
        this.exportExchange = exportExchange;
    }

    public String getExportRoutingKey() {
        return exportRoutingKey;
    }

    public void setExportRoutingKey(String exportRoutingKey) {
        this.exportRoutingKey = exportRoutingKey;
    }

    public String getExportQueue() {
        return exportQueue;
    }

    public void setExportQueue(String exportQueue) {
        this.exportQueue = exportQueue;
    }
}
