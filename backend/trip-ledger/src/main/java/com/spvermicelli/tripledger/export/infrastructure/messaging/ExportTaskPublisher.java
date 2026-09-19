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
        rabbitTemplate.convertAndSend(
            properties.getExportExchange(),
            properties.getExportRoutingKey(),
            new ExportTaskMessage(exportRecordId)
        );
    }
}
