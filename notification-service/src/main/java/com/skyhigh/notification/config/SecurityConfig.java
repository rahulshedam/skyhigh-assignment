package com.skyhigh.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for Notification Service.
 * OTP endpoints are public; all other endpoints require authentication via X-User-Email header.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SimpleHeaderAuthFilter simpleHeaderAuthFilter() {
        return new SimpleHeaderAuthFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(simpleHeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Public: OTP endpoints (part of login flow)
                        .requestMatchers("/api/notifications/otp/**").permitAll()
                        // Public: health check
                        .requestMatchers("/api/notifications/hello").permitAll()
                        // Public: Swagger UI for assignment review
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
