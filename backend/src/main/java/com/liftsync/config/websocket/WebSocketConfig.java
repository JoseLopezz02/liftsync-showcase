package com.liftsync.config.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${env.frontend.allowed-origins}")
    private String allowedOrigins;

    private final AuthHandshakeInterceptor authInterceptor;
    private final ChatWebSocketHandler chatHandler;

    public WebSocketConfig(AuthHandshakeInterceptor authInterceptor, ChatWebSocketHandler chatHandler) {
        this.authInterceptor = authInterceptor;
        this.chatHandler = chatHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        registry
                .addHandler(chatHandler, "/api/ws/chat")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(origins);
    }

}