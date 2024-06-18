package com.image.project.bizmq;

import com.image.project.constant.BiMqConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/9
 */
@Component
public class BiMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY, message);
    }

    public void sendMessageWithUserId(String message) {
        rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_RETRY_ROUTING_KEY, message);
    }

}
