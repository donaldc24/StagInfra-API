package com.stagllc.staginfra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StagInfraApplication {
    private static final Logger logger = LoggerFactory.getLogger(StagInfraApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(StagInfraApplication.class, args);
        logger.info("Server started");
    }
}
