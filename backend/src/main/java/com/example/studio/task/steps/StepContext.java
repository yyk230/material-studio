package com.example.studio.task.steps;

import java.util.List;

import com.example.studio.task.GenerationTask;
import com.example.studio.template.Template;

/** 流水线各步骤共享的上下文。 */
public class StepContext {

    private final GenerationTask task;
    private final Template template;
    private final List<String> inputUploadIds;
    private String resultUrl;

    public StepContext(GenerationTask task, Template template, List<String> inputUploadIds) {
        this.task = task;
        this.template = template;
        this.inputUploadIds = inputUploadIds;
    }

    public GenerationTask task() {
        return task;
    }

    public Template template() {
        return template;
    }

    public List<String> inputUploadIds() {
        return inputUploadIds;
    }

    public String resultUrl() {
        return resultUrl;
    }

    public void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }
}
