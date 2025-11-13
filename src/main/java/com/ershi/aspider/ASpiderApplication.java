package com.ershi.aspider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 *
 * @author Ershi-Gu.
 * @since 2025-11-12
 */
@SpringBootApplication
@EnableScheduling
public class ASpiderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ASpiderApplication.class, args);
    }
}
