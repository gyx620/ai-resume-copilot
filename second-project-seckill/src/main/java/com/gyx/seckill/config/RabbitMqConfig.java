package com.gyx.seckill.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String ORDER_QUEUE = "seckill.order.queue";
    public static final String ORDER_ROUTING_KEY = "seckill.order.create";
    public static final String ORDER_DEAD_EXCHANGE = "seckill.order.dead.exchange";
    public static final String ORDER_DEAD_QUEUE = "seckill.order.dead.queue";
    public static final String ORDER_DEAD_ROUTING_KEY = "seckill.order.dead";

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .deadLetterExchange(ORDER_DEAD_EXCHANGE)
                .deadLetterRoutingKey(ORDER_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillOrderBinding(@Qualifier("seckillOrderQueue") Queue seckillOrderQueue,
                                       @Qualifier("seckillOrderExchange") DirectExchange seckillOrderExchange) {
        return BindingBuilder.bind(seckillOrderQueue)
                .to(seckillOrderExchange)
                .with(ORDER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange seckillOrderDeadExchange() {
        return new DirectExchange(ORDER_DEAD_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderDeadQueue() {
        return QueueBuilder.durable(ORDER_DEAD_QUEUE).build();
    }

    @Bean
    public Binding seckillOrderDeadBinding(@Qualifier("seckillOrderDeadQueue") Queue seckillOrderDeadQueue,
                                           @Qualifier("seckillOrderDeadExchange") DirectExchange seckillOrderDeadExchange) {
        return BindingBuilder.bind(seckillOrderDeadQueue)
                .to(seckillOrderDeadExchange)
                .with(ORDER_DEAD_ROUTING_KEY);
    }
}
