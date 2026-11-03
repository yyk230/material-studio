package com.example.studio.task.steps;

import com.example.studio.task.TaskStage;

public interface PipelineStep {

    TaskStage stage();

    void execute(StepContext ctx) throws Exception;
}
