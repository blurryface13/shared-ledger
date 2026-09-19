package com.spvermicelli.tripledger.shared.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

@Configuration
@EnableConfigurationProperties(RabbitMqExportProperties.class)
public class RabbitMqConfiguration {

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public DirectExchange exportExchange(RabbitMqExportProperties properties) {
        return new DirectExchange(properties.getExportExchange(), true, false);
    }

    @Bean
    public Queue exportQueue(RabbitMqExportProperties properties) {
        return new Queue(properties.getExportQueue(), true);
    }

    @Bean
    public Binding exportBinding(Queue exportQueue, DirectExchange exportExchange, RabbitMqExportProperties properties) {
        return BindingBuilder.bind(exportQueue)
            .to(exportExchange)
            .with(properties.getExportRoutingKey());
    }
}
