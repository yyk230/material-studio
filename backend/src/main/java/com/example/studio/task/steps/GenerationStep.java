package com.example.studio.task.steps;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.example.studio.task.TaskStage;
import com.example.studio.upload.StorageService;

/** 模拟生成：把模板预置的结果图复制到任务工作目录，作为“生成产物”。 */
@Component
public class GenerationStep implements PipelineStep {

    static final String WORK_FILE = "result.svg";

    private final StorageService storage;

    public GenerationStep(StorageService storage) {
        this.storage = storage;
    }

    @Override
    public TaskStage stage() {
        return TaskStage.GENERATION;
    }

    @Override
    public void execute(StepContext ctx) throws Exception {
        Path workDir = storage.workDir(ctx.task().id);
        Files.createDirectories(workDir);
        ClassPathResource preset = new ClassPathResource("assets/results/" + ctx.template().resultAsset);
        try (InputStream in = preset.getInputStream()) {
            Files.copy(in, workDir.resolve(WORK_FILE), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
