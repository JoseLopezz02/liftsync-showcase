// This class configures Spring Security for the application, allowing all requests but enabling CORS with specific settings.
// The CORS configuration allows requests from origins specified in the application properties, and permits common HTTP methods and headers.
package com.liftsync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${env.frontend.allowed-origins}")
    private String allowedOrigins;

    private final LiftSyncAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(LiftSyncAuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session
                        -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(SecurityPaths.publicApiPaths()).permitAll()
                        .requestMatchers(SecurityPaths.WS_PATTERN).permitAll()
                        .requestMatchers(SecurityPaths.API_PATTERN).authenticated()
                        .anyRequest().denyAll()

                )
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                List.of(allowedOrigins.split(","))
        );
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);  // Allow cookies to be sent!

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(SecurityPaths.API_PATTERN, configuration);
        return source;
    }
}
