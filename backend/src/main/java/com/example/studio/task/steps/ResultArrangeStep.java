package com.example.studio.task.steps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Component;

import com.example.studio.task.TaskStage;
import com.example.studio.upload.StorageService;

/** 结果整理：把生成产物移动到结果目录，清理工作目录，产出可预览地址。 */
@Component
public class ResultArrangeStep implements PipelineStep {

    private final StorageService storage;

    public ResultArrangeStep(StorageService storage) {
        this.storage = storage;
    }

    @Override
    public TaskStage stage() {
        return TaskStage.RESULT_ARRANGE;
    }

    @Override
    public void execute(StepContext ctx) throws Exception {
        String taskId = ctx.task().id;
        Path workDir = storage.workDir(taskId);
        Path generated = workDir.resolve(GenerationStep.WORK_FILE);
        if (!Files.isRegularFile(generated)) {
            throw new StepFailedException("未找到生成产物");
        }
        Path target = storage.resultFile(taskId);
        Files.createDirectories(target.getParent());
        Files.move(generated, target, StandardCopyOption.REPLACE_EXISTING);
        storage.deleteRecursively(workDir);
        ctx.setResultUrl("/api/files/results/" + taskId);
    }
}
