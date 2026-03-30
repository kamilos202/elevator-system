package com.example.elevator_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over SockJS endpoint and broker configuration.
 *
 * SockJS is the fallback transport — if a browser doesn't support native WebSockets it'll
 * transparently downgrade to long-polling or XHR streaming. In practice every modern
 * browser supports WebSockets, but it's still a good safety net.
 *
 * Note: setAllowedOriginPatterns("*") must be used here instead of setAllowedOrigins("*").
 * SockJS sends credentialed requests, and Spring will throw an IllegalArgumentException
 * if you combine allowCredentials=true with a literal wildcard origin.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic is where the server pushes status updates
        config.enableSimpleBroker("/topic");
        // /app prefixes messages that should be routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/elevator")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
