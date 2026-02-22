package com.skyhigh.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for payment service.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.payment}")
    private String paymentExchange;

    @Value("${rabbitmq.queues.payment-processed}")
    private String paymentProcessedQueue;

    @Value("${rabbitmq.routing-keys.payment-success}")
    private String paymentSuccessRoutingKey;

    @Value("${rabbitmq.routing-keys.payment-failure}")
    private String paymentFailureRoutingKey;

    /**
     * Payment exchange
     */
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(paymentExchange);
    }

    /**
     * Payment processed queue
     */
    @Bean
    public Queue paymentProcessedQueue() {
        return new Queue(paymentProcessedQueue, true); // durable
    }

    /**
     * Binding for payment success events
     */
    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder
                .bind(paymentProcessedQueue())
                .to(paymentExchange())
                .with(paymentSuccessRoutingKey);
    }

    /**
     * Binding for payment failure events
     */
    @Bean
    public Binding paymentFailureBinding() {
        return BindingBuilder
                .bind(paymentProcessedQueue())
                .to(paymentExchange())
                .with(paymentFailureRoutingKey);
    }

    /**
     * JSON message converter for RabbitMQ
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
