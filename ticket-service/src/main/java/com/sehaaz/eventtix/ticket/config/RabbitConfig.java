package com.sehaaz.eventtix.ticket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.DLQ_SUFFIX;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.DLX;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.EXCHANGE;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_CONFIRMED;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.TICKET_GENERATE_QUEUE;

/**
 * ticket-service'in tükettiği queue: order.confirmed → bilet üret.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange sagaExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    @Bean
    public Declarables ticketGenerateQueue() {
        return sagaQueue(TICKET_GENERATE_QUEUE, ORDER_CONFIRMED);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Queue + DLQ + bağlamalar. Reddedilen mesaj DLX'e queue'nun DLQ adıyla yönlendirilir.
     */
    private static Declarables sagaQueue(String name, String routingKey) {
        String dlqName = name + DLQ_SUFFIX;
        Queue queue = QueueBuilder.durable(name)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(dlqName)
                .build();
        Queue dlq = QueueBuilder.durable(dlqName).build();
        return new Declarables(
                queue,
                dlq,
                new Binding(name, Binding.DestinationType.QUEUE, EXCHANGE, routingKey, null),
                new Binding(dlqName, Binding.DestinationType.QUEUE, DLX, dlqName, null));
    }
}
