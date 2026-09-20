package com.spvermicelli.tripledger.export;

import static org.junit.jupiter.api.Assertions.*;
import com.spvermicelli.tripledger.export.infrastructure.messaging.ExportTaskPublisher;
import com.spvermicelli.tripledger.shared.infrastructure.messaging.RabbitMqExportProperties;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import java.util.UUID;

class ExportPublisherTest {
    CachingConnectionFactory connection;
    RabbitAdmin admin;
    RabbitTemplate template;
    RabbitMqExportProperties properties;
    String name;
    @BeforeEach void setup() {
        connection=new CachingConnectionFactory(System.getenv().getOrDefault("TRIP_LEDGER_RABBITMQ_HOST","127.0.0.1"),Integer.parseInt(System.getenv().getOrDefault("TRIP_LEDGER_RABBITMQ_PORT","5672")));
        connection.setUsername(System.getenv().getOrDefault("TRIP_LEDGER_RABBITMQ_USERNAME","guest"));
        connection.setPassword(System.getenv().getOrDefault("TRIP_LEDGER_RABBITMQ_PASSWORD","guest"));
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        template=new RabbitTemplate(connection);template.setMessageConverter(new JacksonJsonMessageConverter());
        admin=new RabbitAdmin(connection);name="test.export."+UUID.randomUUID();
        admin.declareExchange(new DirectExchange(name,false,true));
        properties=new RabbitMqExportProperties();properties.setExportExchange(name);properties.setExportRoutingKey("task");
    }
    @AfterEach void cleanup() {try {admin.deleteQueue(name);admin.deleteExchange(name);} finally {connection.destroy();}}
    @Test void confirmedRoutableMessageIsPersistent() {
        admin.declareQueue(new Queue(name,false,false,true));
        admin.declareBinding(new Binding(name,Binding.DestinationType.QUEUE,name,"task",null));
        new ExportTaskPublisher(template,properties).publish(42L);
        var message=template.receive(name,2000);assertNotNull(message);
        assertEquals("export:42",message.getMessageProperties().getMessageId());
        assertEquals(MessageDeliveryMode.PERSISTENT,message.getMessageProperties().getReceivedDeliveryMode());
    }
    @Test void unroutableMessageDoesNotCountAsSuccessfulPublish() {
        assertThrows(IllegalStateException.class,()->new ExportTaskPublisher(template,properties).publish(42L));
    }
}
