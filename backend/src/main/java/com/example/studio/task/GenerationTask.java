package com.example.studio.task;

import java.time.Instant;

/** generation_tasks 表行对象；templateTitle 来自查询时 JOIN templates。 */
public class GenerationTask {
    public String id;
    public Long userId;
    public String templateId;
    public String templateTitle;
    public TaskStatus status;
    public TaskStage currentStage;
    public String errorMessage;
    public String resultUrl;
    public TaskStage simFailStage;
    public Integer simStepDelayMs;
    public Instant createdAt;
    public Instant startedAt;
    public Instant finishedAt;
    public Instant updatedAt;
}
