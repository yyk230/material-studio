package com.example.studio.simulation;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.example.studio.config.AppProperties;
import com.example.studio.task.TaskStage;

/** 全局模拟配置；创建任务时取快照写入任务行，之后修改不影响已创建的任务。 */
@Component
public class SimulationSettings {

    public static final int MAX_STEP_DELAY_MS = 60_000;

    public record Snapshot(TaskStage failStage, int stepDelayMs) {
    }

    private final Snapshot initial;
    private final AtomicReference<Snapshot> current;

    public SimulationSettings(AppProperties props) {
        int delay = props.simulation().stepDelayMs();
        if (delay < 0 || delay > MAX_STEP_DELAY_MS) {
            throw new IllegalArgumentException("app.simulation.step-delay-ms 必须在 0~60000 之间");
        }
        this.initial = new Snapshot(props.simulation().failStage(), delay);
        this.current = new AtomicReference<>(initial);
    }

    public Snapshot current() {
        return current.get();
    }

    public Snapshot update(TaskStage failStage, int stepDelayMs) {
        Snapshot next = new Snapshot(failStage, stepDelayMs);
        current.set(next);
        return next;
    }

    public void reset() {
        current.set(initial);
    }
}
