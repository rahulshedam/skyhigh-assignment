package com.skyhigh.baggage.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for baggage event publishing.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.baggage}")
    private String baggageExchange;

    @Value("${rabbitmq.routing-keys.baggage-validated}")
    private String baggageValidatedRoutingKey;

    @Value("${rabbitmq.routing-keys.excess-fee}")
    private String excessFeeRoutingKey;

    @Bean
    public TopicExchange baggageExchange() {
        return new TopicExchange(baggageExchange);
    }

    @Bean
    public Queue baggageValidatedQueue() {
        return new Queue("baggage.validated.queue", true);
    }

    @Bean
    public Queue excessFeeQueue() {
        return new Queue("baggage.excess-fee.queue", true);
    }

    @Bean
    public Binding baggageValidatedBinding() {
        return BindingBuilder
                .bind(baggageValidatedQueue())
                .to(baggageExchange())
                .with(baggageValidatedRoutingKey);
    }

    @Bean
    public Binding excessFeeBinding() {
        return BindingBuilder
                .bind(excessFeeQueue())
                .to(baggageExchange())
                .with(excessFeeRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
