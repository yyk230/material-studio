package com.example.studio.upload;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.studio.auth.CurrentUserId;
import com.example.studio.common.ApiException;
import com.example.studio.common.ErrorCode;
import com.example.studio.task.GenerationTask;
import com.example.studio.task.TaskMapper;
import com.example.studio.task.TaskStatus;

/** 用户素材与结果图访问；不存在与无权限统一返回 404。 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final UploadMapper uploadMapper;
    private final TaskMapper taskMapper;
    private final StorageService storage;

    public FileController(UploadMapper uploadMapper, TaskMapper taskMapper, StorageService storage) {
        this.uploadMapper = uploadMapper;
        this.taskMapper = taskMapper;
        this.storage = storage;
    }

    @GetMapping("/uploads/{id}")
    public ResponseEntity<Resource> upload(@CurrentUserId long userId, @PathVariable String id) {
        UploadAsset asset = uploadMapper.findById(id);
        if (asset == null || asset.userId != userId) {
            throw notFound();
        }
        return serve(storage.resolve(asset.storagePath), asset.mimeType);
    }

    @GetMapping("/results/{taskId}")
    public ResponseEntity<Resource> result(@CurrentUserId long userId, @PathVariable String taskId) {
        GenerationTask task = taskMapper.findByIdAndUser(taskId, userId);
        if (task == null || task.status != TaskStatus.SUCCEEDED) {
            throw notFound();
        }
        return serve(storage.resultFile(taskId), "image/svg+xml");
    }

    private ResponseEntity<Resource> serve(Path file, String contentType) {
        if (!Files.isRegularFile(file)) {
            throw notFound();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox")
                .body(new FileSystemResource(file));
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.FILE_NOT_FOUND, "文件不存在");
    }
}
