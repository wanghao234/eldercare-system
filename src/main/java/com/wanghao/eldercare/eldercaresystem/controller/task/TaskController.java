package com.wanghao.eldercare.eldercaresystem.controller.task;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.BizScoped;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.task.*;
import com.wanghao.eldercare.eldercaresystem.entity.task.*;
import com.wanghao.eldercare.eldercaresystem.mapper.task.*;
import com.wanghao.eldercare.eldercaresystem.service.task.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserUtils currentUserUtils;

    public TaskController(TaskService taskService, CurrentUserUtils currentUserUtils) {
        this.taskService = taskService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping("/my")
    public ApiResponse<TaskListResponse> myTasks(@RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(taskService.listMyTasks(currentUser, status, page, size));
    }

    @GetMapping
    public ApiResponse<TaskListResponse> listTasks(@RequestParam(required = false) Long elderId,
                                                   @RequestParam(required = false) Long assignedTo,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String taskType,
                                                   @RequestParam(required = false) String priority,
                                                   @RequestParam(required = false) String relatedBizType,
                                                   @RequestParam(required = false) Long relatedBizId,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(taskService.listTasks(
                currentUser, elderId, assignedTo, status, taskType, priority, relatedBizType, relatedBizId, from, to, page, size
        ));
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, entityType = "tasks", responseIdPath = "taskId")
    public ApiResponse<TaskListItemDTO> createTask(@Valid @RequestBody CreateTaskRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(taskService.createTask(currentUser, request));
    }

    @PostMapping("/{taskId}/start")
    @Audited(action = AuditAction.TRANSITION, entityType = "tasks", entityIdArg = "taskId", fromValue = "pending|overdue", toValue = "in_progress")
    @BizScoped(type = "task", idParam = "taskId")
    public ApiResponse<TaskListItemDTO> start(@PathVariable Long taskId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(taskService.startTask(currentUser, taskId));
    }

    @PostMapping("/{taskId}/complete")
    @Audited(action = AuditAction.TRANSITION, entityType = "tasks", entityIdArg = "taskId", fromValue = "in_progress|overdue", toValue = "completed")
    @BizScoped(type = "task", idParam = "taskId")
    public ApiResponse<TaskListItemDTO> complete(@PathVariable Long taskId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(taskService.completeTask(currentUser, taskId));
    }

    @PostMapping("/{taskId}/cancel")
    @Audited(action = AuditAction.TRANSITION, entityType = "tasks", entityIdArg = "taskId", fromValue = "pending|in_progress|overdue", toValue = "cancelled")
    @BizScoped(type = "task", idParam = "taskId")
    public ApiResponse<TaskListItemDTO> cancel(@PathVariable Long taskId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(taskService.cancelTask(currentUser, taskId));
    }

    @PostMapping("/{taskId}/reassign")
    @Audited(action = AuditAction.TRANSFER, entityType = "tasks", entityIdArg = "taskId", requestFields = {"assignedTo", "comment"})
    public ApiResponse<TaskListItemDTO> reassign(@PathVariable Long taskId,
                                                 @Valid @RequestBody ReassignTaskRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(taskService.reassignTask(currentUser, taskId, request));
    }
}
