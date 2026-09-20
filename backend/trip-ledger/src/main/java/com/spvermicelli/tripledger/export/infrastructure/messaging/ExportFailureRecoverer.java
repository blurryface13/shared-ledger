package com.spvermicelli.tripledger.export.infrastructure.messaging;

import com.spvermicelli.tripledger.shared.infrastructure.messaging.RabbitMqExportProperties;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/** Preserve exhausted messages without altering arguments of an existing business queue. */
@Component
public class ExportFailureRecoverer implements MessageRecoverer {
    private final RabbitTemplate template;
    private final RabbitMqExportProperties properties;
    public ExportFailureRecoverer(RabbitTemplate template, RabbitMqExportProperties properties) {
        this.template = template;
        this.properties = properties;
    }
    @Override
    public void recover(Message message, Throwable cause) {
        var retained = MessageBuilder.fromClonedMessage(message)
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .setHeader("failure-type", cause.getClass().getSimpleName())
            .setHeader("original-queue", properties.getExportQueue())
            .build();
        var correlation = new CorrelationData();
        try {
            template.setMandatory(true);
            template.send(properties.getExportExchange() + ".failed", "failed", retained, correlation);
            var confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.ack() || correlation.getReturned() != null) {
                throw new IllegalStateException("Failure message not routed or confirmed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImmediateRequeueAmqpException("Failure retention interrupted; keep original message", e);
        } catch (Exception e) {
            // No silent acknowledgement when the failure sink itself is unavailable.
            throw new ImmediateRequeueAmqpException("Failure retention unavailable; keep original message", e);
        }
    }
}
