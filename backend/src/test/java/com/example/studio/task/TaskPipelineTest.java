package com.example.studio.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.studio.IntegrationTestBase;
import com.example.studio.common.Times;
import com.example.studio.upload.StorageService;
import com.example.studio.upload.UploadMapper;

import jakarta.servlet.http.Cookie;

/** 直接同步调用流水线，验证步骤顺序、失败中断与幂等。 */
class TaskPipelineTest extends IntegrationTestBase {

    @Autowired
    private TaskPipeline pipeline;
    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private TaskStepMapper stepMapper;
    @Autowired
    private UploadMapper uploadMapper;
    @Autowired
    private StorageService storage;

    @Test
    void runsAllStepsInOrderAndSucceeds() throws Exception {
        String taskId = insertTask(null);

        pipeline.run(taskId);

        List<TaskStep> steps = stepMapper.findByTaskId(taskId);
        assertThat(steps).extracting(s -> s.stage)
                .containsExactly(TaskStage.MATERIAL_CHECK, TaskStage.GENERATION, TaskStage.RESULT_ARRANGE);
        assertThat(steps).allSatisfy(s -> {
            assertThat(s.status).isEqualTo(StepStatus.SUCCEEDED);
            assertThat(s.finishedAt).isNotNull();
        });
        GenerationTask task = taskMapper.findById(taskId);
        assertThat(task.status).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(task.resultUrl).isEqualTo("/api/files/results/" + taskId);
        assertThat(task.errorMessage).isNull();
        assertThat(Files.isRegularFile(storage.resultFile(taskId))).isTrue();
        assertThat(Files.exists(storage.workDir(taskId))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TaskStage.class)
    void stopsAtTheFailedStage(TaskStage failStage) throws Exception {
        String taskId = insertTask(failStage);

        pipeline.run(taskId);

        GenerationTask task = taskMapper.findById(taskId);
        assertThat(task.status).isEqualTo(TaskStatus.FAILED);
        assertThat(task.currentStage).isEqualTo(failStage);
        assertThat(task.errorMessage).isEqualTo(failStage.simulatedFailureMessage());
        assertThat(task.resultUrl).isNull();
        assertThat(task.finishedAt).isNotNull();

        List<TaskStep> steps = stepMapper.findByTaskId(taskId);
        assertThat(steps).hasSize(failStage.ordinal() + 1);
        TaskStep last = steps.get(steps.size() - 1);
        assertThat(last.stage).isEqualTo(failStage);
        assertThat(last.status).isEqualTo(StepStatus.FAILED);
        assertThat(last.message).isEqualTo(failStage.simulatedFailureMessage());
        assertThat(steps.subList(0, steps.size() - 1)).allSatisfy(s -> assertThat(s.status).isEqualTo(StepStatus.SUCCEEDED));
    }

    @Test
    void failsMaterialCheckWhenInputFileIsMissing() throws Exception {
        String taskId = insertTask(null);
        String uploadId = taskMapper.findInputUploadIds(taskId).get(0);
        Files.delete(storage.resolve(uploadMapper.findById(uploadId).storagePath));

        pipeline.run(taskId);

        GenerationTask task = taskMapper.findById(taskId);
        assertThat(task.status).isEqualTo(TaskStatus.FAILED);
        assertThat(task.currentStage).isEqualTo(TaskStage.MATERIAL_CHECK);
        assertThat(task.errorMessage).isEqualTo("素材文件缺失或已损坏");
        assertThat(stepMapper.findByTaskId(taskId)).hasSize(1);
    }

    @Test
    void doesNotRunTwice() throws Exception {
        String taskId = insertTask(null);
        pipeline.run(taskId);
        GenerationTask first = taskMapper.findById(taskId);

        pipeline.run(taskId);

        GenerationTask second = taskMapper.findById(taskId);
        assertThat(second.status).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(second.finishedAt).isEqualTo(first.finishedAt);
        assertThat(stepMapper.findByTaskId(taskId)).hasSize(3);
    }

    @Test
    void queriesDoNotChangeTerminalState() throws Exception {
        Cookie session = register("pipeline-query@example.com");
        String uploadId = upload(session, "pet-sticker", png());
        String taskId = json(createTask(session, "pet-sticker", List.of(uploadId))).get("id").asText();
        awaitTerminal(session, taskId);

        for (int i = 0; i < 3; i++) {
            assertThat(getTask(session, taskId).get("status").asText()).isEqualTo("SUCCEEDED");
        }
        assertThat(stepMapper.findByTaskId(taskId)).hasSize(3);
    }

    /** 准备一个带素材的 PENDING 任务（模拟延迟为 0），不经过线程池。 */
    private String insertTask(TaskStage failStage) throws Exception {
        Cookie session = register("pipeline-" + System.nanoTime() + "@example.com");
        String uploadId = upload(session, "pet-sticker", png());
        Long userId = uploadMapper.findById(uploadId).userId;

        GenerationTask task = new GenerationTask();
        task.id = java.util.UUID.randomUUID().toString();
        task.userId = userId;
        task.templateId = "pet-sticker";
        task.status = TaskStatus.PENDING;
        task.simFailStage = failStage;
        task.simStepDelayMs = 0;
        task.createdAt = Times.now();
        task.updatedAt = task.createdAt;
        taskMapper.insert(task);
        taskMapper.insertInput(task.id, 0, uploadId);
        return task.id;
    }
}
