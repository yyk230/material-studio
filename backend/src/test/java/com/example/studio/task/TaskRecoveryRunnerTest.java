package com.example.studio.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.studio.IntegrationTestBase;

class TaskRecoveryRunnerTest extends IntegrationTestBase {

    @Autowired
    private TaskRecoveryRunner runner;
    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private TaskStepMapper stepMapper;

    @Test
    void failsOnlyTasksCreatedBeforeBoot() {
        Instant boot = runner.bootTime();
        String stale = insert(TaskStatus.PROCESSING, boot.minusSeconds(60));
        stepMapper.insertRunning(stale, TaskStage.GENERATION, boot.minusSeconds(30));
        String fresh = insert(TaskStatus.PENDING, boot.plusSeconds(1));

        int recovered = runner.recover();

        assertThat(recovered).isEqualTo(1);
        GenerationTask staleTask = taskMapper.findById(stale);
        assertThat(staleTask.status).isEqualTo(TaskStatus.FAILED);
        assertThat(staleTask.errorMessage).isEqualTo("服务重启，任务已中断");
        assertThat(stepMapper.findByTaskId(stale)).singleElement()
                .satisfies(step -> assertThat(step.status).isEqualTo(StepStatus.FAILED));
        assertThat(taskMapper.findById(fresh).status).isEqualTo(TaskStatus.PENDING);
    }

    private String insert(TaskStatus status, Instant createdAt) {
        GenerationTask task = new GenerationTask();
        task.id = UUID.randomUUID().toString();
        task.userId = 1L;
        task.templateId = "pet-sticker";
        task.status = status;
        task.simStepDelayMs = 0;
        task.createdAt = createdAt;
        task.updatedAt = createdAt;
        taskMapper.insert(task);
        return task.id;
    }
}
