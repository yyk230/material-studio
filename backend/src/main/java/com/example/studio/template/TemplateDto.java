package com.example.studio.template;

import java.util.List;

public record TemplateDto(
        String id,
        String title,
        String category,
        String coverUrl,
        String description,
        int requiredImageCount,
        List<String> acceptedMimeTypes,
        long maxFileSizeBytes) {

    static TemplateDto from(Template t) {
        return new TemplateDto(t.id, t.title, t.category, t.coverUrl, t.description,
                t.requiredImageCount, t.acceptedMimeTypeList(), t.maxFileSizeBytes);
    }
}
