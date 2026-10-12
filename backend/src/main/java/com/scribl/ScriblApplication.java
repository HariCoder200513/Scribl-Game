package com.scribl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScriblApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScriblApplication.class, args);
    }
}
