package com.wanghao.eldercare.eldercaresystem.controller.workflow;

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
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.*;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.workflow.*;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflows")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_DOCTOR,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final CurrentUserUtils currentUserUtils;

    public WorkflowController(WorkflowService workflowService, CurrentUserUtils currentUserUtils) {
        this.workflowService = workflowService;
        this.currentUserUtils = currentUserUtils;
    }

    @PostMapping("/instances")
    public ApiResponse<CreateWfInstanceResponse> createInstance(@Valid @RequestBody CreateWfInstanceRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.createInstance(currentUser, request));
    }

    @GetMapping("/instances")
    public ApiResponse<WfInstanceDetailDTO> getInstance(@RequestParam String bizType,
                                                         @RequestParam Long bizId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.getInstanceByBiz(currentUser, bizType, bizId));
    }

    @GetMapping("/tasks/my")
    public ApiResponse<WfTaskListResponse> myTasks(@RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        List<String> statuses = StringUtils.hasText(status)
                ? Arrays.stream(status.split(",")).map(String::trim).filter(StringUtils::hasText).toList()
                : null;
        return ApiResponse.ok(workflowService.listMyTasks(currentUser, statuses, page, size));
    }

    @PostMapping("/tasks/{wfTaskId}/claim")
    @Audited(action = AuditAction.CLAIM, entityType = "wf_tasks", entityIdArg = "wfTaskId")
    public ApiResponse<WfTaskDTO> claim(@PathVariable Long wfTaskId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.claim(currentUser, wfTaskId));
    }

    @PostMapping("/tasks/{wfTaskId}/complete")
    @Audited(action = AuditAction.COMPLETE, entityType = "wf_tasks", entityIdArg = "wfTaskId")
    public ApiResponse<WfTaskDTO> complete(@PathVariable Long wfTaskId,
                                           @Valid @RequestBody CompleteWfTaskRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.complete(currentUser, wfTaskId, request));
    }
}
