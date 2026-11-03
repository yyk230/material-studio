package com.example.studio.task;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.studio.common.Times;

/**
 * 启动时把上次运行遗留的未完成任务标记为失败（不做断点续跑）。
 * 只处理 bootTime 之前创建的任务，避免误伤 Web 服务已启动后新提交的任务。假设后端单实例部署。
 */
@Component
public class TaskRecoveryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskRecoveryRunner.class);
    static final String MESSAGE = "服务重启，任务已中断";

    private final TaskMapper taskMapper;
    private final Instant bootTime = Times.now();

    public TaskRecoveryRunner(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public Instant bootTime() {
        return bootTime;
    }

    @Override
    public void run(ApplicationArguments args) {
        recover();
    }

    public int recover() {
        Instant now = Times.now();
        taskMapper.failRunningStepsCreatedBefore(bootTime, MESSAGE, now);
        int count = taskMapper.failUnfinishedCreatedBefore(bootTime, MESSAGE, now);
        if (count > 0) {
            log.warn("marked {} unfinished task(s) as FAILED after restart", count);
        }
        return count;
    }
}
