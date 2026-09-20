package com.spvermicelli.tripledger.export.infrastructure.messaging;

import com.spvermicelli.tripledger.shared.infrastructure.messaging.RabbitMqExportProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExportTaskPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqExportProperties properties;

    public ExportTaskPublisher(RabbitTemplate rabbitTemplate, RabbitMqExportProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(Long exportRecordId) {
        var correlation = new org.springframework.amqp.rabbit.connection.CorrelationData(java.util.UUID.randomUUID().toString());
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.convertAndSend(properties.getExportExchange(), properties.getExportRoutingKey(),
            new ExportTaskMessage(exportRecordId), message -> {
                message.getMessageProperties().setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setMessageId("export:" + exportRecordId);
                return message;
            }, correlation);
        try {
            var confirm = correlation.getFuture().get(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("Broker rejected or could not route export task");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Export publishing interrupted", e);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("Export publishing not confirmed", e);
        }
    }
}
