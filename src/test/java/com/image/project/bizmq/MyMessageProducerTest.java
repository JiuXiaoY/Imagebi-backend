package com.image.project.bizmq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/9
 */
@SpringBootTest
class MyMessageProducerTest {

    // 使用 @Resource 注解注入一个消息生产者的实例
    @Resource
    private BiMessageProducer myMessageProducer;

    // 使用 @Test 注解标记一个测试方法，命名为 sendMessage
    /**
     * 在测试方法中，调用消息生产者的 sendMessage 方法，发送一条消息
     *
     * 参数1：交换机名称为 "code_exchange"，表示将消息发送到该交换机
     * 参数2：路由键为 "my_routingKey"，表示消息将通过该路由键进行路由
     * 参数3：消息内容为 "你好呀"，即要发送的具体消息内容
     */
    @Test
    void sendMessage() {
        // 调用消息生产者的 sendMessage 方法发送消息
        // myMessageProducer.sendMessage("code_exchange", "my_routingKey", "你好呀");
    }
}