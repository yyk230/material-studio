package com.example.studio.task;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.studio.common.ApiException;
import com.example.studio.common.ErrorCode;
import com.example.studio.common.Times;
import com.example.studio.simulation.SimulationSettings;
import com.example.studio.template.Template;
import com.example.studio.template.TemplateService;
import com.example.studio.upload.UploadAsset;
import com.example.studio.upload.UploadMapper;

@Service
public class TaskService {

    public static final int MAX_PAGE_SIZE = 50;

    private final TemplateService templateService;
    private final UploadMapper uploadMapper;
    private final TaskMapper taskMapper;
    private final TaskStepMapper stepMapper;
    private final SimulationSettings simulation;

    public TaskService(TemplateService templateService, UploadMapper uploadMapper, TaskMapper taskMapper,
                       TaskStepMapper stepMapper, SimulationSettings simulation) {
        this.templateService = templateService;
        this.uploadMapper = uploadMapper;
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.simulation = simulation;
    }

    /** 校验并创建 PENDING 任务；方法返回时事务已提交，之后再由调用方派发执行。 */
    @Transactional
    public TaskDto create(long userId, String templateId, List<String> uploadIds) {
        Template template = templateService.require(templateId);
        if (uploadIds.size() != template.requiredImageCount) {
            throw new ApiException(ErrorCode.IMAGE_COUNT_MISMATCH,
                    "该模板需要 " + template.requiredImageCount + " 张图片，实际提交 " + uploadIds.size() + " 张");
        }
        if (new HashSet<>(uploadIds).size() != uploadIds.size()) {
            throw new ApiException(ErrorCode.INVALID_UPLOAD, "存在重复的图片");
        }
        List<String> accepted = template.acceptedMimeTypeList();
        for (String uploadId : uploadIds) {
            UploadAsset asset = uploadId == null ? null : uploadMapper.findById(uploadId);
            if (asset == null || asset.userId != userId || !asset.templateId.equals(template.id)
                    || !accepted.contains(asset.mimeType)) {
                throw new ApiException(ErrorCode.INVALID_UPLOAD, "图片不存在或不可用于该模板，请重新上传");
            }
        }

        SimulationSettings.Snapshot sim = simulation.current();
        Instant now = Times.now();
        GenerationTask task = new GenerationTask();
        task.id = UUID.randomUUID().toString();
        task.userId = userId;
        task.templateId = template.id;
        task.templateTitle = template.title;
        task.status = TaskStatus.PENDING;
        task.simFailStage = sim.failStage();
        task.simStepDelayMs = sim.stepDelayMs();
        task.createdAt = now;
        task.updatedAt = now;
        taskMapper.insert(task);
        for (int i = 0; i < uploadIds.size(); i++) {
            taskMapper.insertInput(task.id, i, uploadIds.get(i));
        }
        return TaskDto.from(task, List.of());
    }

    public TaskDto.Page list(long userId, String templateId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "分页参数不合法");
        }
        List<GenerationTask> rows = taskMapper.findPage(userId, templateId, size + 1, page * size);
        boolean hasMore = rows.size() > size;
        List<GenerationTask> items = hasMore ? rows.subList(0, size) : rows;
        if (items.isEmpty()) {
            return new TaskDto.Page(List.of(), false);
        }
        Map<String, List<TaskStep>> stepsByTask = stepMapper
                .findByTaskIds(items.stream().map(t -> t.id).toList())
                .stream()
                .collect(Collectors.groupingBy(s -> s.taskId));
        List<TaskDto> dtos = items.stream()
                .map(t -> TaskDto.from(t, stepsByTask.getOrDefault(t.id, List.of())))
                .toList();
        return new TaskDto.Page(dtos, hasMore);
    }

    public TaskDto get(long userId, String taskId) {
        GenerationTask task = taskMapper.findByIdAndUser(taskId, userId);
        if (task == null) {
            throw new ApiException(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        return TaskDto.from(task, stepMapper.findByTaskId(taskId));
    }
}
