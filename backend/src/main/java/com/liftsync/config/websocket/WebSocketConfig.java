// This class is responsible for configuring the WebSocket endpoints for the LiftSync application.
// It defines the WebSocket handler for the chat service and registers the necessary interceptors for authentication and CORS handling.
package com.liftsync.config.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${env.frontend.allowed-origins}")
    private String allowedOrigins;

    private final AuthHandshakeInterceptor authInterceptor;

    public WebSocketConfig(AuthHandshakeInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        registry
                .addHandler(chatHandler(), "/api/ws/chat")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(origins);
    }

    @Bean
    public WebSocketHandler chatHandler() {
        return new ChatWebSocketHandler();
    }
}




