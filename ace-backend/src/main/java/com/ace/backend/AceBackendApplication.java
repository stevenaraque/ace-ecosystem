package com.ace.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AceBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(AceBackendApplication.class, args);
    }
}
