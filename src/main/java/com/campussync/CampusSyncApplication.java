package com.campussync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampusSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusSyncApplication.class, args);
    }
}
