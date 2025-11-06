package com.ershi.aspider;

import com.ershi.aspider.datasource.service.ElasticsearchTestService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ASpiderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ASpiderApplication.class, args);
    }

    @Bean
    public CommandLineRunner testElasticsearch(ElasticsearchTestService esTestService) {
        return args -> {
            esTestService.testConnection();
        };
    }

}
