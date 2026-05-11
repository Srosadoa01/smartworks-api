package com.smartworks.smartworks_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.smartworks.smartworks_api")
public class SmartworksApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartworksApiApplication.class, args);
    }
}


