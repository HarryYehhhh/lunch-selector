package com.lunch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LunchSelectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(LunchSelectorApplication.class, args);
    }
}
