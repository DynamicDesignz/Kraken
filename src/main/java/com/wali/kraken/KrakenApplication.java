package com.wali.kraken;

import com.wali.kraken.config.PreStartupDependencyConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories
@EnableAsync
@EnableScheduling
public class KrakenApplication {

    public static void main(String[] args) {
        SpringApplication.run(KrakenApplication.class, args);
    }
}
