package com.example.passwordgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PasswordProperties.class)
public class PasswordGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PasswordGeneratorApplication.class, args);
    }
}
