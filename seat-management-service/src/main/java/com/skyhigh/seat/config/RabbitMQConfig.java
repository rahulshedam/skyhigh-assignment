package com.skyhigh.seat.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for seat management events.
 */
@Configuration
public class RabbitMQConfig {

    // Exchange name
    public static final String SEAT_EXCHANGE = "seat.exchange";

    // Queue names
    public static final String SEAT_HELD_QUEUE = "seat.held.queue";
    public static final String SEAT_CONFIRMED_QUEUE = "seat.confirmed.queue";
    public static final String SEAT_RELEASED_QUEUE = "seat.released.queue";
    public static final String WAITLIST_ASSIGNED_QUEUE = "waitlist.assigned.queue";

    // Routing keys
    public static final String SEAT_HELD_ROUTING_KEY = "seat.held";
    public static final String SEAT_CONFIRMED_ROUTING_KEY = "seat.confirmed";
    public static final String SEAT_RELEASED_ROUTING_KEY = "seat.released";
    public static final String WAITLIST_ASSIGNED_ROUTING_KEY = "waitlist.assigned";

    @Bean
    public TopicExchange seatExchange() {
        return new TopicExchange(SEAT_EXCHANGE);
    }

    @Bean
    public Queue seatHeldQueue() {
        return new Queue(SEAT_HELD_QUEUE, true);
    }

    @Bean
    public Queue seatConfirmedQueue() {
        return new Queue(SEAT_CONFIRMED_QUEUE, true);
    }

    @Bean
    public Queue seatReleasedQueue() {
        return new Queue(SEAT_RELEASED_QUEUE, true);
    }

    @Bean
    public Queue waitlistAssignedQueue() {
        return new Queue(WAITLIST_ASSIGNED_QUEUE, true);
    }

    @Bean
    public Binding seatHeldBinding() {
        return BindingBuilder
                .bind(seatHeldQueue())
                .to(seatExchange())
                .with(SEAT_HELD_ROUTING_KEY);
    }

    @Bean
    public Binding seatConfirmedBinding() {
        return BindingBuilder
                .bind(seatConfirmedQueue())
                .to(seatExchange())
                .with(SEAT_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding seatReleasedBinding() {
        return BindingBuilder
                .bind(seatReleasedQueue())
                .to(seatExchange())
                .with(SEAT_RELEASED_ROUTING_KEY);
    }

    @Bean
    public Binding waitlistAssignedBinding() {
        return BindingBuilder
                .bind(waitlistAssignedQueue())
                .to(seatExchange())
                .with(WAITLIST_ASSIGNED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
