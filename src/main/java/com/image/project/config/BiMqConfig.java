package com.image.project.config;

import com.image.project.constant.BiMqConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置 MQ
 *
 * @author JXY
 * @version 1.0
 * @since 2024/6/9
 */
@Configuration
public class BiMqConfig {
    /**
     * 创建一个直接交换机 bi_exchange
     *
     * @return 返回
     */
    @Bean
    DirectExchange directExchange() {
        return ExchangeBuilder.directExchange(BiMqConstant.BI_EXCHANGE_NAME).durable(true).build();
    }

    /**
     * 声明一个死信交换机
     *
     * @return 返回
     */
    @Bean
    DirectExchange directExchangeDead() {
        return ExchangeBuilder.directExchange(BiMqConstant.BI_DEAD_EXCHANGE_NAME).durable(true).build();
    }


    /**
     * 创建一个队列 bi_queue
     *
     * @return 返回
     */
    @Bean(BiMqConstant.BI_QUEUE_NAME)
    Queue biQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-message-ttl",10_000);
        arguments.put("x-dead-letter-exchange",BiMqConstant.BI_DEAD_EXCHANGE_NAME);
        arguments.put("x-dead-letter-routing-key",BiMqConstant.BI_DEAD_ROUTING_KEY);
        return QueueBuilder.durable(BiMqConstant.BI_QUEUE_NAME).withArguments(arguments).build();
    }

    /**
     * 创建一个队列 bi_retry_queue
     *
     * @return 返回
     */
    @Bean(BiMqConstant.BI_RETRY_QUEUE_NAME)
    Queue biRetryQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-message-ttl",10_000);
        arguments.put("x-dead-letter-exchange",BiMqConstant.BI_DEAD_EXCHANGE_NAME);
        arguments.put("x-dead-letter-routing-key",BiMqConstant.BI_DEAD_ROUTING_KEY);
        return QueueBuilder.durable(BiMqConstant.BI_RETRY_QUEUE_NAME).withArguments(arguments).build();
    }

    @Bean(BiMqConstant.BI_DEAD_QUEUE_NAME)
    Queue biDeadQueue() {
        return QueueBuilder.durable(BiMqConstant.BI_DEAD_QUEUE_NAME).build();
    }


    /**
     * 创建一个绑定关系 bi_routingKey
     *
     * @return 返回
     */
    @Bean
    Binding bi_binding() {
        return BindingBuilder.bind(biQueue()).to(directExchange()).with(BiMqConstant.BI_ROUTING_KEY);
    }

    /**
     * 创建一个绑定关系 bi_routingKey
     *
     * @return 返回
     */
    @Bean
    Binding bi_retry_binding() {
        return BindingBuilder.bind(biRetryQueue()).to(directExchange()).with(BiMqConstant.BI_RETRY_ROUTING_KEY);
    }

    @Bean
    Binding bi_dead_binding() {
        return BindingBuilder.bind(biDeadQueue()).to(directExchangeDead()).with(BiMqConstant.BI_DEAD_ROUTING_KEY);
    }
}
