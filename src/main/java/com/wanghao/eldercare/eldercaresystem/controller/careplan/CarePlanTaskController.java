package com.wanghao.eldercare.eldercaresystem.controller.careplan;

import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.BatchUpdateCarePlanTaskAssigneeRequest;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.BatchUpdateCarePlanTaskTimeRequest;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.BatchDeleteCarePlanTasksRequest;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.CarePlanTaskStatisticsResponse;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.CarePlanTaskDTO;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.CompleteCarePlanTaskRequest;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.ConfirmCarePlanTasksResponse;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.CreateCarePlanTaskRequest;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.DeleteCarePlanTasksResponse;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.UpdateCarePlanTaskRequest;
import com.wanghao.eldercare.eldercaresystem.service.careplan.CarePlanTaskService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/care-plan-tasks")
public class CarePlanTaskController {

    private final CarePlanTaskService carePlanTaskService;
    private final CurrentUserUtils currentUserUtils;

    public CarePlanTaskController(CarePlanTaskService carePlanTaskService,
                                  CurrentUserUtils currentUserUtils) {
        this.carePlanTaskService = carePlanTaskService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping("/by-plan/{carePlanId}")
    public ApiResponse<List<CarePlanTaskDTO>> listByPlan(@PathVariable Long carePlanId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanTaskService.listByPlan(currentUser, carePlanId));
    }

    @GetMapping("/my")
    public ApiResponse<List<CarePlanTaskDTO>> listMyTasks() {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanTaskService.listMyTasks(currentUser));
    }

    @GetMapping("/overdue")
    public ApiResponse<List<CarePlanTaskDTO>> listOverdueTasks() {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanTaskService.listOverdueTasks(currentUser));
    }

    @GetMapping("/statistics")
    public ApiResponse<CarePlanTaskStatisticsResponse> getStatistics(@RequestParam(required = false) LocalDate startDate,
                                                                    @RequestParam(required = false) LocalDate endDate,
                                                                    @RequestParam(required = false) Long elderId,
                                                                    @RequestParam(required = false) Long assignedNurseId,
                                                                    @RequestParam(required = false) Long carePlanId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(carePlanTaskService.getStatistics(currentUser, startDate, endDate, elderId, assignedNurseId, carePlanId));
    }

    @PostMapping
    public ApiResponse<CarePlanTaskDTO> createTask(@Valid @RequestBody CreateCarePlanTaskRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("护理任务草稿创建成功", carePlanTaskService.createTask(currentUser, request));
    }

    @PostMapping("/confirm/{carePlanId}")
    public ApiResponse<ConfirmCarePlanTasksResponse> confirmTasks(@PathVariable Long carePlanId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        ConfirmCarePlanTasksResponse response = carePlanTaskService.confirmTasks(currentUser, carePlanId);
        return ApiResponse.success(response.getMessage(), response);
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<DeleteCarePlanTasksResponse> deleteTask(@PathVariable Long taskId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        DeleteCarePlanTasksResponse response = carePlanTaskService.deleteTask(currentUser, taskId);
        return ApiResponse.success(response.getMessage(), response);
    }

    @DeleteMapping("/batch")
    public ApiResponse<DeleteCarePlanTasksResponse> batchDeleteTasksV2(@Valid @RequestBody BatchDeleteCarePlanTasksRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        DeleteCarePlanTasksResponse response = carePlanTaskService.batchDeleteTasks(currentUser, request);
        return ApiResponse.success(response.getMessage(), response);
    }

    @PostMapping("/batch-delete")
    public ApiResponse<DeleteCarePlanTasksResponse> batchDeleteTasks(@Valid @RequestBody BatchDeleteCarePlanTasksRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        DeleteCarePlanTasksResponse response = carePlanTaskService.batchDeleteTasks(currentUser, request);
        return ApiResponse.success(response.getMessage(), response);
    }

    @PostMapping("/batch-confirm")
    public ApiResponse<ConfirmCarePlanTasksResponse> batchConfirmTasks(@Valid @RequestBody BatchDeleteCarePlanTasksRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        ConfirmCarePlanTasksResponse response = carePlanTaskService.batchConfirmTasks(currentUser, request);
        return ApiResponse.success(response.getMessage(), response);
    }

    @PutMapping("/{taskId}")
    public ApiResponse<CarePlanTaskDTO> updateTask(@PathVariable Long taskId,
                                                   @Valid @RequestBody UpdateCarePlanTaskRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("护理任务修改成功", carePlanTaskService.updateTask(currentUser, taskId, request));
    }

    @PutMapping("/batch-time")
    public ApiResponse<List<CarePlanTaskDTO>> batchUpdateTaskTime(@Valid @RequestBody BatchUpdateCarePlanTaskTimeRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("护理任务时间批量修改成功", carePlanTaskService.batchUpdateTaskTime(currentUser, request));
    }

    @PutMapping("/batch-assignee")
    public ApiResponse<List<CarePlanTaskDTO>> batchUpdateTaskAssignee(@Valid @RequestBody BatchUpdateCarePlanTaskAssigneeRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("护理任务护理人员批量修改成功", carePlanTaskService.batchUpdateTaskAssignee(currentUser, request));
    }

    @PutMapping("/{taskId}/complete")
    public ApiResponse<CarePlanTaskDTO> completeTask(@PathVariable Long taskId,
                                                     @Valid @RequestBody CompleteCarePlanTaskRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("护理任务完成成功", carePlanTaskService.completeTask(currentUser, taskId, request));
    }
}
