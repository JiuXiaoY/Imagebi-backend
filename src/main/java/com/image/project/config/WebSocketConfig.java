package com.image.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/13
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig {

    /**
     * ServerEndpointExporter是一个Spring组件，负责扫描带有@ServerEndpoint注解的类并注册它们为WebSocket端点。
     * @return null
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}
