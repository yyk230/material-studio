package com.example.studio.task;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.studio.auth.CurrentUserId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    public record CreateTaskRequest(@NotBlank String templateId, @NotNull List<String> uploadIds) {
    }

    private final TaskService taskService;
    private final TaskDispatcher dispatcher;

    public TaskController(TaskService taskService, TaskDispatcher dispatcher) {
        this.taskService = taskService;
        this.dispatcher = dispatcher;
    }

    /** 创建记录后立即返回 202，处理在后台线程中进行。 */
    @PostMapping
    public ResponseEntity<TaskDto> create(@CurrentUserId long userId, @Valid @RequestBody CreateTaskRequest request) {
        TaskDto task = taskService.create(userId, request.templateId(), request.uploadIds());
        dispatcher.dispatch(task.id());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(task);
    }

    @GetMapping
    public TaskDto.Page list(@CurrentUserId long userId,
                             @RequestParam(required = false) String templateId,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return taskService.list(userId, templateId, page, size);
    }

    @GetMapping("/{id}")
    public TaskDto get(@CurrentUserId long userId, @PathVariable String id) {
        return taskService.get(userId, id);
    }
}
