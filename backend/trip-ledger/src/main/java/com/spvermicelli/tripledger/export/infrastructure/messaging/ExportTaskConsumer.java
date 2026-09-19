package com.spvermicelli.tripledger.export.infrastructure.messaging;

import com.spvermicelli.tripledger.export.application.ExportApplicationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ExportTaskConsumer {

    private final ExportApplicationService exportApplicationService;

    public ExportTaskConsumer(ExportApplicationService exportApplicationService) {
        this.exportApplicationService = exportApplicationService;
    }

    @RabbitListener(queues = "${app.rabbitmq.export-queue}")
    public void consume(ExportTaskMessage message) {
        exportApplicationService.processExportTask(message.exportRecordId());
    }
}
