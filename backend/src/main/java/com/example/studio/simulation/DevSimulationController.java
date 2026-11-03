package com.example.studio.simulation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.studio.task.TaskStage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 开发用模拟控制接口，仅在 app.dev-api.enabled=true 时注册。 */
@RestController
@RequestMapping("/api/dev/simulation")
@ConditionalOnProperty(prefix = "app.dev-api", name = "enabled", havingValue = "true")
public class DevSimulationController {

    public record UpdateRequest(
            TaskStage failStage,
            @NotNull @Min(0) @Max(SimulationSettings.MAX_STEP_DELAY_MS) Integer stepDelayMs) {
    }

    private final SimulationSettings settings;

    public DevSimulationController(SimulationSettings settings) {
        this.settings = settings;
    }

    @GetMapping
    public SimulationSettings.Snapshot get() {
        return settings.current();
    }

    @PutMapping
    public SimulationSettings.Snapshot update(@Valid @RequestBody UpdateRequest request) {
        return settings.update(request.failStage(), request.stepDelayMs());
    }
}
