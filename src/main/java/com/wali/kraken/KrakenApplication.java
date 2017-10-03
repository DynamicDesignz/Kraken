package com.wali.kraken;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class KrakenApplication {

	public static void main(String[] args) {
		SpringApplication.run(KrakenApplication.class, args);
	}

}
