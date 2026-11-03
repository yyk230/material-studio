package com.example.studio.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.example.studio.task.TaskStage;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Storage storage, Auth auth, Task task, Simulation simulation) {

    public record Storage(String root) {
    }

    public record Auth(Duration sessionTtl, boolean cookieSecure) {
    }

    public record Task(int workerThreads, int queueCapacity) {
    }

    public record Simulation(int stepDelayMs, TaskStage failStage) {
    }
}
