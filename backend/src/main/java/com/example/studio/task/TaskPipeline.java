package com.example.studio.task;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.studio.task.steps.PipelineStep;
import com.example.studio.task.steps.StepContext;
import com.example.studio.task.steps.StepFailedException;
import com.example.studio.template.Template;
import com.example.studio.template.TemplateMapper;

/**
 * 在工作线程中按 素材检查 → 模拟生成 → 结果整理 顺序执行任务。
 * 每一步开始/结束都单独落库；任一步失败立即停止，后续步骤不再执行。
 */
@Component
public class TaskPipeline {

    private static final Logger log = LoggerFactory.getLogger(TaskPipeline.class);
    static final String INTERRUPTED_MESSAGE = "服务停止，任务中断";

    private final TaskMapper taskMapper;
    private final TemplateMapper templateMapper;
    private final TaskStateService state;
    private final List<PipelineStep> steps;

    public TaskPipeline(TaskMapper taskMapper, TemplateMapper templateMapper, TaskStateService state,
                        List<PipelineStep> steps) {
        this.taskMapper = taskMapper;
        this.templateMapper = templateMapper;
        this.state = state;
        this.steps = steps.stream().sorted(Comparator.comparing(PipelineStep::stage)).toList();
    }

    public void run(String taskId) {
        if (!state.markProcessing(taskId)) {
            return;
        }
        StepContext ctx;
        try {
            GenerationTask task = taskMapper.findById(taskId);
            Template template = templateMapper.findById(task.templateId);
            ctx = new StepContext(task, template, taskMapper.findInputUploadIds(taskId));
        } catch (Exception e) {
            log.error("task={} failed to load context", taskId, e);
            safely(() -> state.failTask(taskId, "任务数据加载失败"));
            return;
        }

        GenerationTask task = ctx.task();
        for (PipelineStep step : steps) {
            TaskStage stage = step.stage();
            String failure = null;
            boolean interrupted = false;
            try {
                state.startStep(taskId, stage);
                Thread.sleep(task.simStepDelayMs);
                if (stage == task.simFailStage) {
                    throw new StepFailedException(stage.simulatedFailureMessage());
                }
                step.execute(ctx);
                state.completeStep(taskId, stage);
            } catch (StepFailedException e) {
                failure = e.getMessage();
            } catch (InterruptedException e) {
                failure = INTERRUPTED_MESSAGE;
                interrupted = true;
            } catch (Exception e) {
                log.error("task={} stage={} unexpected error", taskId, stage, e);
                failure = stage.label() + "执行异常";
            }

            if (failure != null) {
                String message = failure;
                // 先写库再恢复中断标记：线程处于中断状态时连接池会拒绝分配连接
                safely(() -> {
                    state.failStep(taskId, stage, message);
                    state.failTask(taskId, message);
                });
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
        }
        safely(() -> state.succeedTask(taskId, ctx.resultUrl()));
    }

    private void safely(Runnable write) {
        try {
            write.run();
        } catch (Exception e) {
            // 例如停机时连接池已关闭；残留状态由下次启动的 TaskRecoveryRunner 收尾
            log.error("failed to persist task state", e);
        }
    }
}
