package com.example.studio.task;

import java.time.Instant;

/** task_steps 表行对象。 */
public class TaskStep {
    public Long id;
    public String taskId;
    public TaskStage stage;
    public StepStatus status;
    public String message;
    public Instant startedAt;
    public Instant finishedAt;
}
