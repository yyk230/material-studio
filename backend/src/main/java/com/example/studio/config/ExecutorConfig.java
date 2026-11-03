package com.example.studio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

    @Bean(name = "taskWorkerExecutor")
    public ThreadPoolTaskExecutor taskWorkerExecutor(AppProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.task().workerThreads());
        executor.setMaxPoolSize(props.task().workerThreads());
        executor.setQueueCapacity(props.task().queueCapacity());
        executor.setThreadNamePrefix("task-worker-");
        // 未完成的任务在下次启动时由 TaskRecoveryRunner 标记为中断
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
