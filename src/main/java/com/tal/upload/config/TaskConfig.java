package com.tal.upload.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/26  9:22
 * @Description:
 */
@Configuration
@EnableAsync
public class TaskConfig {

    @Value("${task.async.pool.corePoolSize}")
    int corePoolSize;
    @Value("${task.async.pool.maxPoolSize}")
    int maxPoolSize;
    @Value("${task.async.pool.queueCapacity}")
    int queueCapacity;
    @Bean("AsyncTaskExecutor")
    public Executor taskExecutor(){

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("AsyncUploadTaskExecutor-");
        executor.initialize();
        return executor;

    }
}
