package com.wanghao.eldercare.eldercaresystem.controller.careplan;

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
import com.wanghao.eldercare.eldercaresystem.dto.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.*;
import com.wanghao.eldercare.eldercaresystem.service.careplan.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CarePlanController {

    private final CarePlanService carePlanService;
    private final CarePlanTaskService carePlanTaskService;
    private final CurrentUserUtils currentUserUtils;

    public CarePlanController(CarePlanService carePlanService,
                              CarePlanTaskService carePlanTaskService,
                              CurrentUserUtils currentUserUtils) {
        this.carePlanService = carePlanService;
        this.carePlanTaskService = carePlanTaskService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping("/care-plans")
    public ApiResponse<CarePlanListResponse> listCarePlans(@RequestParam(required = false) Long elderId,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanService.listCarePlans(currentUser, elderId, status, page, size));
    }

    @GetMapping("/care-plans/{id}")
    public ApiResponse<CarePlanDTO> getCarePlan(@PathVariable Long id) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanService.getCarePlan(currentUser, id));
    }

    @GetMapping("/care-plans/{carePlanId}/execution-report")
    public ApiResponse<CarePlanExecutionReportResponse> getExecutionReport(@PathVariable Long carePlanId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanTaskService.getExecutionReport(currentUser, carePlanId));
    }

    @PostMapping("/care-plans")
    @Audited(action = AuditAction.CREATE, entityType = "care_plans", responseIdPath = "carePlanId",
            requestFields = {"elderId", "version", "status", "startDate", "endDate", "careTime", "medicationReminder", "dietPlan"})
    public ApiResponse<CarePlanDTO> createCarePlan(@Valid @RequestBody UpsertCarePlanRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanService.createCarePlan(currentUser, request));
    }

    @PutMapping("/care-plans/{id}")
    @Audited(action = AuditAction.UPDATE, entityType = "care_plans", entityIdArg = "id",
            requestFields = {"elderId", "version", "status", "startDate", "endDate", "careTime", "medicationReminder", "dietPlan"})
    public ApiResponse<CarePlanDTO> updateCarePlan(@PathVariable Long id,
                                                   @Valid @RequestBody UpsertCarePlanRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanService.updateCarePlan(currentUser, id, request));
    }

    @DeleteMapping("/care-plans/{id}")
    @Audited(action = AuditAction.DELETE, entityType = "care_plans", entityIdArg = "id")
    public ApiResponse<Void> deleteCarePlan(@PathVariable Long id) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        carePlanService.deleteCarePlan(currentUser, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/care-plan-changes")
    @Audited(action = AuditAction.CREATE, entityType = "care_plan_change_requests", responseIdPath = "id",
            requestFields = {"elderId", "draftPlanId", "changeType", "requiresDoctorReview", "reason", "proposedTitle", "proposedContent"})
    public ApiResponse<IdResponse> createChange(@Valid @RequestBody CreateCarePlanChangeRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanService.createChange(currentUser, request));
    }

    @GetMapping("/care-plan-changes")
    public ApiResponse<CarePlanChangeListResponse> listChanges(@RequestParam(required = false) Long elderId,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanService.listChanges(currentUser, elderId, status, page, size));
    }

    @GetMapping("/care-plan-changes/{id}")
    public ApiResponse<CarePlanChangeDTO> detail(@PathVariable Long id) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanService.getChangeDetail(currentUser, id));
    }

    @PostMapping("/care-plan-changes/{id}/approve")
    @Audited(action = AuditAction.APPROVE, entityType = "care_plan_change_requests", entityIdArg = "id",
            fromValue = "pending", toValue = "approved", requestFields = {"comment"})
    public ApiResponse<CarePlanChangeDTO> approve(@PathVariable Long id,
                                                  @RequestBody(required = false) ReviewCarePlanChangeRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanService.approve(currentUser, id, request));
    }

    @PostMapping("/care-plan-changes/{id}/reject")
    @Audited(action = AuditAction.REJECT, entityType = "care_plan_change_requests", entityIdArg = "id",
            fromValue = "pending", toValue = "rejected", requestFields = {"comment"})
    public ApiResponse<CarePlanChangeDTO> reject(@PathVariable Long id,
                                                 @RequestBody(required = false) ReviewCarePlanChangeRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanService.reject(currentUser, id, request));
    }

    @PostMapping("/care-plans/{id}/regenerate-tasks")
    @Audited(action = AuditAction.UPDATE, entityType = "tasks", entityIdArg = "id")
    public ApiResponse<RegenerateTasksResponse> regenerateTasks(@PathVariable Long id,
                                                                @RequestParam(defaultValue = "7") int days) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanService.regenerateTasks(currentUser, id, days));
    }

    @PostMapping("/care-plans/{carePlanId}/generate-tasks")
    @Audited(action = AuditAction.CREATE, entityType = "care_plan_tasks", entityIdArg = "carePlanId")
    public ApiResponse<GenerateCarePlanTasksResponse> generateCarePlanTasks(@PathVariable Long carePlanId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("护理任务生成成功", carePlanTaskService.generateTasks(currentUser, carePlanId));
    }

    @PostMapping("/care-plans/{carePlanId}/confirm-tasks")
    @Audited(action = AuditAction.UPDATE, entityType = "care_plan_tasks", entityIdArg = "carePlanId")
    public ApiResponse<ConfirmCarePlanTasksResponse> confirmCarePlanTasks(@PathVariable Long carePlanId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        ConfirmCarePlanTasksResponse response = carePlanTaskService.confirmTasks(currentUser, carePlanId);
        return ApiResponse.success(response.getMessage(), response);
    }

    @PostMapping("/care-plans/{carePlanId}/auto-assign-tasks")
    @Audited(action = AuditAction.UPDATE, entityType = "care_plan_tasks", entityIdArg = "carePlanId")
    public ApiResponse<AutoAssignCarePlanTasksResponse> autoAssignCarePlanTasks(@PathVariable Long carePlanId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        AutoAssignCarePlanTasksResponse response = carePlanTaskService.autoAssignTasks(currentUser, carePlanId);
        return ApiResponse.success(response.getMessage(), response);
    }
}
