package com.arcaneiceman.kraken.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration implements AsyncConfigurer {

    private final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    private int corePoolSize;

    private int maxPoolSize;

    private int queueCapacity;

    private String threadName;

    public AsyncConfiguration(Environment environment) {
        corePoolSize = Integer.parseInt(environment.getProperty("application.async.core-pool-size", "5"));
        maxPoolSize = Integer.parseInt(environment.getProperty("application.async.max-pool-size", "5"));
        queueCapacity = Integer.parseInt(environment.getProperty("application.async.queue-capacity", "100"));
        threadName = environment.getProperty("application.async.thread-name", "fastfinancebox-async-thread");
    }

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        log.debug("Creating Async Task Executor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadName);
        return new SimpleAsyncTaskExecutor(executor);
    }
}
