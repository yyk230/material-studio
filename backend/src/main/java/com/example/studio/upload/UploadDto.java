package com.example.studio.upload;

public record UploadDto(String id, String url, String mimeType, long sizeBytes, String originalName) {

    static UploadDto from(UploadAsset asset) {
        return new UploadDto(asset.id, "/api/files/uploads/" + asset.id, asset.mimeType, asset.sizeBytes,
                asset.originalName);
    }
}
