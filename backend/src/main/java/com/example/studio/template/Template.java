package com.example.studio.template;

import java.util.Arrays;
import java.util.List;

/** templates 表行对象。 */
public class Template {
    public String id;
    public String title;
    public String category;
    public String coverUrl;
    public String description;
    public Integer requiredImageCount;
    public String acceptedMimeTypes;
    public Long maxFileSizeBytes;
    public String resultAsset;
    public Integer sortOrder;

    public List<String> acceptedMimeTypeList() {
        return Arrays.stream(acceptedMimeTypes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
