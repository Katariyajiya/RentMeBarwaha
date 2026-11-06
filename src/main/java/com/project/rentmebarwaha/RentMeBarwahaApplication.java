package com.project.rentmebarwaha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RentMeBarwahaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentMeBarwahaApplication.class, args);
    }
}

