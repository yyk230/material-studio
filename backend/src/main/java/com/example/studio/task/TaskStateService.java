package com.example.studio.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.studio.common.Times;

/**
 * 任务状态的所有写入入口。独立成 bean 是为了让 {@link #startStep} 的 @Transactional 经过代理生效；
 * 其余方法都是单条带状态条件的 SQL，终态不会被改回。
 */
@Service
public class TaskStateService {

    private static final Logger log = LoggerFactory.getLogger(TaskStateService.class);

    private final TaskMapper taskMapper;
    private final TaskStepMapper stepMapper;

    public TaskStateService(TaskMapper taskMapper, TaskStepMapper stepMapper) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
    }

    /** PENDING → PROCESSING；返回 false 表示任务已被处理过（或不存在）。 */
    public boolean markProcessing(String taskId) {
        boolean updated = taskMapper.markProcessing(taskId, Times.now()) == 1;
        if (updated) {
            log.info("task={} status=PROCESSING", taskId);
        }
        return updated;
    }

    @Transactional
    public void startStep(String taskId, TaskStage stage) {
        if (taskMapper.updateStage(taskId, stage, Times.now()) != 1) {
            throw new IllegalStateException("task " + taskId + " is no longer PROCESSING");
        }
        stepMapper.insertRunning(taskId, stage, Times.now());
        log.info("task={} stage={} status=RUNNING", taskId, stage);
    }

    public void completeStep(String taskId, TaskStage stage) {
        stepMapper.markSucceeded(taskId, stage, Times.now());
        log.info("task={} stage={} status=SUCCEEDED", taskId, stage);
    }

    public void failStep(String taskId, TaskStage stage, String message) {
        stepMapper.markFailed(taskId, stage, message, Times.now());
        log.info("task={} stage={} status=FAILED message={}", taskId, stage, message);
    }

    public void failTask(String taskId, String message) {
        taskMapper.markFailed(taskId, message, Times.now());
        log.info("task={} status=FAILED message={}", taskId, message);
    }

    public void succeedTask(String taskId, String resultUrl) {
        taskMapper.markSucceeded(taskId, resultUrl, Times.now());
        log.info("task={} status=SUCCEEDED resultUrl={}", taskId, resultUrl);
    }

    public void failIfPending(String taskId, String message) {
        if (taskMapper.failIfPending(taskId, message, Times.now()) == 1) {
            log.warn("task={} status=FAILED message={}", taskId, message);
        }
    }
}
