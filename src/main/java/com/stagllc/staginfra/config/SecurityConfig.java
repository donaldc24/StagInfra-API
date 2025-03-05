// src/main/java/com/stagllc/staginfra/config/SecurityConfig.java
package com.stagllc.staginfra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("!test") // Only active when not in test profile
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for API endpoints
                .csrf(csrf -> csrf.disable())

                // Configure session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/cost/**").permitAll() // Allow cost endpoints without authentication
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/", "/signup", "/login", "/verify-email", "/public/**").permitAll()
                        // Protected endpoints
                        .anyRequest().authenticated())

                // Configure form login (for browser-based clients)
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll())

                // Configure logout
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .permitAll())

                // Disable HTTP Basic Auth
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}