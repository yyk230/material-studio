package com.example.studio.task.steps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.studio.task.TaskStage;
import com.example.studio.upload.FileTypeDetector;
import com.example.studio.upload.StorageService;
import com.example.studio.upload.UploadAsset;
import com.example.studio.upload.UploadMapper;

/** 重新确认每个输入素材仍然存在且与上传时记录一致。 */
@Component
public class MaterialCheckStep implements PipelineStep {

    static final String BROKEN_MESSAGE = "素材文件缺失或已损坏";

    private final UploadMapper uploadMapper;
    private final StorageService storage;

    public MaterialCheckStep(UploadMapper uploadMapper, StorageService storage) {
        this.uploadMapper = uploadMapper;
        this.storage = storage;
    }

    @Override
    public TaskStage stage() {
        return TaskStage.MATERIAL_CHECK;
    }

    @Override
    public void execute(StepContext ctx) throws Exception {
        if (ctx.inputUploadIds().size() != ctx.template().requiredImageCount) {
            throw new StepFailedException(BROKEN_MESSAGE);
        }
        for (String uploadId : ctx.inputUploadIds()) {
            UploadAsset asset = uploadMapper.findById(uploadId);
            if (asset == null) {
                throw new StepFailedException(BROKEN_MESSAGE);
            }
            Path file = storage.resolve(asset.storagePath);
            if (!Files.isRegularFile(file) || Files.size(file) != asset.sizeBytes) {
                throw new StepFailedException(BROKEN_MESSAGE);
            }
            Optional<String> type = FileTypeDetector.detect(file);
            if (type.isEmpty() || !type.get().equals(asset.mimeType)) {
                throw new StepFailedException(BROKEN_MESSAGE);
            }
        }
    }
}
