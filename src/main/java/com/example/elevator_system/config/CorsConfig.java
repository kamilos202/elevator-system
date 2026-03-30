package com.example.elevator_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Opens up cross-origin requests so the Angular dev server (localhost:4200) can talk
 * to the Spring backend (localhost:8080) without the browser blocking it.
 *
 * allowedOriginPatterns("*") is intentional — the same reason as in WebSocketConfig.
 * Using allowedOrigins("*") alongside allowCredentials would cause Spring to reject
 * the config at startup with an IllegalArgumentException.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
