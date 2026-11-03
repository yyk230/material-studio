package com.example.studio.task;

import java.time.Instant;
import java.util.List;

public record TaskDto(
        String id,
        String templateId,
        String templateTitle,
        TaskStatus status,
        TaskStage currentStage,
        String errorMessage,
        String resultUrl,
        List<StepDto> steps,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {

    public record StepDto(TaskStage stage, StepStatus status, String message, Instant startedAt, Instant finishedAt) {

        static StepDto from(TaskStep step) {
            return new StepDto(step.stage, step.status, step.message, step.startedAt, step.finishedAt);
        }
    }

    public record Page(List<TaskDto> items, boolean hasMore) {
    }

    static TaskDto from(GenerationTask t, List<TaskStep> steps) {
        return new TaskDto(t.id, t.templateId, t.templateTitle, t.status, t.currentStage, t.errorMessage,
                t.resultUrl, steps.stream().map(StepDto::from).toList(), t.createdAt, t.startedAt, t.finishedAt);
    }
}
