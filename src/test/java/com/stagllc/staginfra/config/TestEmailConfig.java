// src/test/java/com/stagllc/staginfra/config/TestEmailConfig.java
package com.stagllc.staginfra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@Profile("test")
public class TestEmailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }
}