package com.example.studio.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.studio.common.ApiException;
import com.example.studio.common.ErrorCode;
import com.example.studio.common.Times;
import com.example.studio.template.Template;
import com.example.studio.template.TemplateService;

@Service
public class UploadService {

    private final TemplateService templateService;
    private final UploadMapper uploadMapper;
    private final StorageService storage;

    public UploadService(TemplateService templateService, UploadMapper uploadMapper, StorageService storage) {
        this.templateService = templateService;
        this.uploadMapper = uploadMapper;
        this.storage = storage;
    }

    public UploadDto upload(long userId, String templateId, MultipartFile file) throws IOException {
        Template template = templateService.require(templateId);
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.FILE_EMPTY, "文件为空");
        }
        if (file.getSize() > template.maxFileSizeBytes) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE, "图片不能超过 " + formatMb(template.maxFileSizeBytes) + " MB");
        }
        List<String> accepted = template.acceptedMimeTypeList();
        Optional<String> detected;
        try (InputStream in = file.getInputStream()) {
            detected = FileTypeDetector.detect(in);
        }
        if (detected.isEmpty() || !accepted.contains(detected.get())) {
            String allowed = accepted.stream().map(FileTypeDetector::displayName).collect(Collectors.joining("/"));
            throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE, "仅支持 " + allowed + " 格式的图片");
        }
        String mimeType = detected.get();
        String id = UUID.randomUUID().toString();
        String storagePath;
        try (InputStream in = file.getInputStream()) {
            storagePath = storage.saveUpload(userId, id + "." + FileTypeDetector.extension(mimeType), in);
        }

        UploadAsset asset = new UploadAsset();
        asset.id = id;
        asset.userId = userId;
        asset.templateId = template.id;
        asset.originalName = originalName(file);
        asset.mimeType = mimeType;
        asset.sizeBytes = file.getSize();
        asset.storagePath = storagePath;
        asset.createdAt = Times.now();
        uploadMapper.insert(asset);
        return UploadDto.from(asset);
    }

    private static String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "image";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static String formatMb(long bytes) {
        double mb = bytes / 1024.0 / 1024.0;
        return mb == Math.floor(mb) ? String.valueOf((long) mb) : String.format("%.1f", mb);
    }
}
