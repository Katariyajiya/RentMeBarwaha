package com.project.rentmebarwaha.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${nestearth.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${nestearth.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${nestearth.cors.allowed-headers:Authorization,Content-Type,X-Correlation-ID}")
    private String allowedHeaders;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false);
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedHeaders(Arrays.stream(allowedHeaders.split(",")).map(String::trim).toList());
        config.setAllowedMethods(Arrays.stream(allowedMethods.split(",")).map(String::trim).toList());
        config.setExposedHeaders(List.of("X-Correlation-ID"));
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

