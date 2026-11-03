package com.example.studio.task;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/** 把已提交的任务交给后台线程池；必须在事务之外调用。 */
@Component
public class TaskDispatcher {

    static final String QUEUE_FULL_MESSAGE = "任务队列已满，请稍后再试";

    private final ThreadPoolTaskExecutor executor;
    private final TaskPipeline pipeline;
    private final TaskStateService state;

    public TaskDispatcher(@Qualifier("taskWorkerExecutor") ThreadPoolTaskExecutor executor,
                          TaskPipeline pipeline, TaskStateService state) {
        this.executor = executor;
        this.pipeline = pipeline;
        this.state = state;
    }

    public void dispatch(String taskId) {
        try {
            executor.execute(() -> pipeline.run(taskId));
        } catch (TaskRejectedException e) {
            state.failIfPending(taskId, QUEUE_FULL_MESSAGE);
        }
    }
}
