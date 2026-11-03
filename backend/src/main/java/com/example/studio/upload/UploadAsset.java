package com.example.studio.upload;

import java.time.Instant;

/** upload_assets 表行对象。 */
public class UploadAsset {
    public String id;
    public Long userId;
    public String templateId;
    public String originalName;
    public String mimeType;
    public Long sizeBytes;
    public String storagePath;
    public Instant createdAt;
}
