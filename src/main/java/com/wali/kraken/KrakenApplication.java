package com.wali.kraken;

import org.h2.server.web.WebServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaRepositories
@EnableAsync
public class KrakenApplication {

    public static void main(String[] args) {
        SpringApplication.run(KrakenApplication.class, args);
    }


    @Configuration
    public class BeanConfig {
        @Bean
        ServletRegistrationBean h2servletRegistration() {
            ServletRegistrationBean registrationBean =
                    new ServletRegistrationBean(new WebServlet());
            registrationBean.addUrlMappings("/console/*");
            return registrationBean;
        }
    }
}
