package com.wanghao.eldercare.eldercaresystem.service.careplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.BatchUpdateCarePlanTaskAssigneeRequest;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.BatchUpdateCarePlanTaskTimeRequest;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.AutoAssignCarePlanTasksResponse;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.BatchDeleteCarePlanTasksRequest;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.CarePlanExecutionReportResponse;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.CarePlanTaskDTO;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.CarePlanTaskStatisticsResponse;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.CompleteCarePlanTaskRequest;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.ConfirmCarePlanTasksResponse;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.CreateCarePlanTaskRequest;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.DeleteCarePlanTasksResponse;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.GenerateCarePlanTasksResponse;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.UpdateCarePlanTaskRequest;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.CarePlan;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.CarePlanTask;
import com.wanghao.eldercare.eldercaresystem.entity.shift.StaffShiftSchedule;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.CarePlanRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.CarePlanTaskRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.notification.NotificationRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.shift.StaffShiftScheduleRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.ai.AiProperties;
import com.wanghao.eldercare.eldercaresystem.service.ai.OpenAiCompatibleAiClient;
import com.wanghao.eldercare.eldercaresystem.service.notification.NotificationService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CarePlanTaskService {

    private static final Logger log = LoggerFactory.getLogger(CarePlanTaskService.class);

    private final CarePlanRepository carePlanRepository;
    private final CarePlanTaskRepository carePlanTaskRepository;
    private final CareTeamAssignmentRepository careTeamAssignmentRepository;
    private final StaffShiftScheduleRepository staffShiftScheduleRepository;
    private final PermissionService permissionService;
    private final OpenAiCompatibleAiClient aiClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CarePlanTaskScheduleGenerator carePlanTaskScheduleGenerator;

    public CarePlanTaskService(CarePlanRepository carePlanRepository,
                               CarePlanTaskRepository carePlanTaskRepository,
                               CareTeamAssignmentRepository careTeamAssignmentRepository,
                               StaffShiftScheduleRepository staffShiftScheduleRepository,
                               PermissionService permissionService,
                               OpenAiCompatibleAiClient aiClient,
                               AiProperties aiProperties,
                               ObjectMapper objectMapper,
                               NotificationService notificationService,
                               NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               CarePlanTaskScheduleGenerator carePlanTaskScheduleGenerator) {
        this.carePlanRepository = carePlanRepository;
        this.carePlanTaskRepository = carePlanTaskRepository;
        this.careTeamAssignmentRepository = careTeamAssignmentRepository;
        this.staffShiftScheduleRepository = staffShiftScheduleRepository;
        this.permissionService = permissionService;
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.carePlanTaskScheduleGenerator = carePlanTaskScheduleGenerator;
    }

    @Transactional
    public GenerateCarePlanTasksResponse generateTasks(CurrentUser currentUser, Long carePlanId) {
        requireTaskManager(currentUser);
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());
        TaskGenerationOutcome outcome = generateTasksInternal(plan, true);
        if (!outcome.isTaskGenerated()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, outcome.getTaskGenerateMessage(), HttpStatus.BAD_REQUEST);
        }
        return toResponse(outcome);
    }

    @Transactional
    public TaskGenerationOutcome autoGenerateTasksAfterCarePlanSaved(CurrentUser currentUser, CarePlan plan) {
        if (plan == null || plan.getCarePlanId() == null) {
            return TaskGenerationOutcome.skipped(null, "护理计划未成功保存，跳过自动生成任务");
        }
        try {
            return generateTasksInternal(plan, false);
        } catch (Exception ex) {
            log.warn("护理计划保存后自动生成任务失败，carePlanId={}, error={}", plan.getCarePlanId(), ex.getMessage());
            return TaskGenerationOutcome.skipped(plan.getCarePlanId(), "护理计划保存成功，但护理任务自动生成失败");
        }
    }

    @Transactional(readOnly = true)
    public List<CarePlanTaskDTO> listByPlan(CurrentUser currentUser, Long carePlanId) {
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());
        return carePlanTaskRepository.findAllByCarePlanIdOrderByScheduledAtAscCreatedAtAscTaskIdAsc(carePlanId)
                .stream()
                .map(CarePlanTaskDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CarePlanTaskDTO> listMyTasks(CurrentUser currentUser) {
        requireTaskExecutor(currentUser);
        return carePlanTaskRepository.findAllByAssignedNurseIdAndStatusNotInOrderByScheduledAtAscCreatedAtAscTaskIdAsc(
                        currentUser.getUserId(),
                        List.of("draft", "cancelled"))
                .stream()
                .map(CarePlanTaskDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CarePlanTaskDTO> listOverdueTasks(CurrentUser currentUser) {
        requireTaskExecutor(currentUser);
        return filterAccessibleTasks(currentUser, carePlanTaskRepository.findAllByStatusIgnoreCaseOrderByScheduledAtAscCreatedAtAscTaskIdAsc("pending"))
                .stream()
                .filter(this::isTaskOverdue)
                .map(CarePlanTaskDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CarePlanTaskStatisticsResponse getStatistics(CurrentUser currentUser,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        Long elderId,
                                                        Long assignedNurseId,
                                                        Long carePlanId) {
        requireTaskExecutor(currentUser);
        List<CarePlanTask> tasks = resolveTaskScope(currentUser, carePlanId, elderId, assignedNurseId);
        List<CarePlanTask> filteredTasks = tasks.stream()
                .filter(task -> startDate == null || !resolveTaskDate(task).isBefore(startDate))
                .filter(task -> endDate == null || !resolveTaskDate(task).isAfter(endDate))
                .toList();

        CarePlanTaskStatisticsResponse response = new CarePlanTaskStatisticsResponse();
        response.setTotalCount(filteredTasks.size());
        response.setDraftCount(filteredTasks.stream().filter(task -> "draft".equalsIgnoreCase(task.getStatus())).count());
        response.setPendingCount(filteredTasks.stream().filter(task -> "pending".equalsIgnoreCase(task.getStatus())).count());
        response.setCompletedCount(filteredTasks.stream().filter(task -> "completed".equalsIgnoreCase(task.getStatus())).count());
        response.setCancelledCount(filteredTasks.stream().filter(task -> isCancelled(task.getStatus())).count());
        response.setOverdueCount(filteredTasks.stream().filter(this::isTaskOverdue).count());
        response.setCompletionRate(calculateCompletionRate(filteredTasks));
        return response;
    }

    @Transactional(readOnly = true)
    public CarePlanExecutionReportResponse getExecutionReport(CurrentUser currentUser, Long carePlanId) {
        requireTaskExecutor(currentUser);
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());

        List<CarePlanTask> tasks = carePlanTaskRepository.findAllByCarePlanIdOrderByScheduledAtAscCreatedAtAscTaskIdAsc(carePlanId);
        CarePlanExecutionReportResponse response = new CarePlanExecutionReportResponse();
        response.setCarePlanId(plan.getCarePlanId());
        response.setElderId(plan.getElderId());
        response.setElderName(resolveUserDisplayName(plan.getElderId()));
        response.setStartDate(plan.getStartDate());
        response.setEndDate(plan.getEndDate());
        response.setTotalTaskCount(tasks.size());
        response.setCompletedTaskCount(tasks.stream().filter(task -> "completed".equalsIgnoreCase(task.getStatus())).count());
        response.setPendingTaskCount(tasks.stream().filter(task -> "pending".equalsIgnoreCase(task.getStatus())).count());
        response.setOverdueTaskCount(tasks.stream().filter(this::isTaskOverdue).count());
        response.setCancelledTaskCount(tasks.stream().filter(task -> isCancelled(task.getStatus())).count());
        response.setCompletionRate(calculateCompletionRate(tasks));
        response.setTasksByType(buildTypeSummaries(tasks));
        response.setTasksByDate(buildDateSummaries(tasks));
        response.setRecentExecutionRecords(buildRecentExecutionRecords(tasks));
        response.setOverdueTasks(tasks.stream().filter(this::isTaskOverdue).map(CarePlanTaskDTO::from).toList());
        response.setSummaryText(buildExecutionSummaryText(response));
        return response;
    }

    @Transactional
    public CarePlanTaskDTO createTask(CurrentUser currentUser, CreateCarePlanTaskRequest request) {
        requireTaskConfirmer(currentUser);
        CarePlan plan = carePlanRepository.findById(request.getCarePlanId())
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());

        Long assignedNurseId = request.getAssignedNurseId();
        if (assignedNurseId == null) {
            assignedNurseId = careTeamAssignmentRepository.findActiveNurseIdsByElderId(plan.getElderId())
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        CarePlanTask task = new CarePlanTask();
        task.setCarePlanId(plan.getCarePlanId());
        task.setElderId(plan.getElderId());
        task.setAssignedNurseId(assignedNurseId);
        task.setTaskType(normalizeTaskType(request.getTaskType()));
        task.setTaskTitle(request.getTaskTitle().trim());
        task.setTaskContent(trimToEmpty(request.getTaskContent()));
        task.setFrequencyDesc(firstNonBlank(trimToEmpty(request.getFrequencyDesc()), resolveFrequency(plan)));
        task.setSuggestedTime(resolveSuggestedTime(trimToEmpty(request.getSuggestedTime()), plan.getCareTime(), normalizeTaskTypeCode(request.getTaskType())));
        applyTaskSchedule(task, request.getScheduledDate(), request.getScheduledTime(), request.getScheduledAt());
        task.setTaskSource("manual");
        task.setStatus(normalizeMutableStatus(request.getStatus(), "draft"));
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        if (task.getAssignedNurseId() == null) {
            autoAssignTasksBySchedule(List.of(task), plan.getElderId());
        }
        return CarePlanTaskDTO.from(carePlanTaskRepository.save(task));
    }

    @Transactional
    public CarePlanTaskDTO completeTask(CurrentUser currentUser, Long taskId, CompleteCarePlanTaskRequest request) {
        requireTaskExecutor(currentUser);
        CarePlanTask task = carePlanTaskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("护理任务不存在"));
        if (!currentUser.hasRole("admin")
                && !currentUser.hasRole("nurse_leader")
                && !currentUser.getUserId().equals(task.getAssignedNurseId())) {
            throw new AccessDeniedException("无权限完成该护理任务");
        }
        if ("completed".equalsIgnoreCase(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该护理任务已完成", HttpStatus.BAD_REQUEST);
        }
        if ("cancelled".equalsIgnoreCase(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该护理任务已取消，无法完成", HttpStatus.BAD_REQUEST);
        }
        if ("draft".equalsIgnoreCase(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该护理任务尚未确认分配，无法完成", HttpStatus.BAD_REQUEST);
        }
        LocalDateTime now = LocalDateTime.now();
        task.setExecutionResult(request.getExecutionResult().trim());
        task.setStatus("completed");
        task.setExecutedAt(now);
        task.setUpdatedAt(now);
        return CarePlanTaskDTO.from(carePlanTaskRepository.save(task));
    }

    @Transactional
    public CarePlanTaskDTO updateTask(CurrentUser currentUser, Long taskId, UpdateCarePlanTaskRequest request) {
        requireTaskConfirmer(currentUser);
        CarePlanTask task = carePlanTaskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("护理任务不存在"));
        permissionService.assertCanAccessElder(currentUser, task.getElderId());
        String currentStatus = safe(task.getStatus()).toLowerCase(Locale.ROOT);
        if ("cancelled".equals(currentStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已取消的护理任务不允许修改", HttpStatus.BAD_REQUEST);
        }
        task.setTaskTitle(request.getTaskTitle().trim());
        task.setTaskType(normalizeTaskType(firstNonBlank(request.getTaskType(), task.getTaskType())));
        task.setTaskContent(trimToEmpty(request.getTaskContent()));
        task.setFrequencyDesc(trimToEmpty(request.getFrequencyDesc()));
        task.setSuggestedTime(trimToEmpty(request.getSuggestedTime()));
        if (!"completed".equals(currentStatus)) {
            applyTaskSchedule(task, request.getScheduledDate(), request.getScheduledTime(), request.getScheduledAt());
            task.setAssignedNurseId(request.getAssignedNurseId());
        } else if (request.getScheduledDate() != null
                || request.getScheduledTime() != null
                || request.getScheduledAt() != null
                || request.getAssignedNurseId() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已完成任务不允许修改执行时间或分配人员", HttpStatus.BAD_REQUEST);
        }
        task.setStatus(normalizeMutableStatus(request.getStatus(), task.getStatus()));
        task.setUpdatedAt(LocalDateTime.now());
        return CarePlanTaskDTO.from(carePlanTaskRepository.save(task));
    }

    @Transactional
    public DeleteCarePlanTasksResponse deleteTask(CurrentUser currentUser, Long taskId) {
        requireTaskConfirmer(currentUser);
        CarePlanTask task = carePlanTaskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("护理任务不存在"));
        permissionService.assertCanAccessElder(currentUser, task.getElderId());
        int deletedCount = softDeleteTask(task);
        DeleteCarePlanTasksResponse response = new DeleteCarePlanTasksResponse();
        response.setDeletedCount(deletedCount);
        response.setMessage(buildDeleteMessage(deletedCount));
        return response;
    }

    @Transactional
    public DeleteCarePlanTasksResponse batchDeleteTasks(CurrentUser currentUser, BatchDeleteCarePlanTasksRequest request) {
        requireTaskConfirmer(currentUser);
        int deletedCount = 0;
        for (Long taskId : request.getTaskIds()) {
            if (taskId == null) {
                continue;
            }
            CarePlanTask task = carePlanTaskRepository.findById(taskId)
                    .orElseThrow(() -> new NotFoundException("护理任务不存在"));
            permissionService.assertCanAccessElder(currentUser, task.getElderId());
            deletedCount += softDeleteTask(task);
        }
        DeleteCarePlanTasksResponse response = new DeleteCarePlanTasksResponse();
        response.setDeletedCount(deletedCount);
        response.setMessage(buildDeleteMessage(deletedCount));
        return response;
    }

    @Transactional
    public ConfirmCarePlanTasksResponse batchConfirmTasks(CurrentUser currentUser, BatchDeleteCarePlanTasksRequest request) {
        requireTaskConfirmer(currentUser);
        List<CarePlanTask> draftTasks = new ArrayList<>();
        for (Long taskId : request.getTaskIds()) {
            if (taskId == null) {
                continue;
            }
            CarePlanTask task = carePlanTaskRepository.findById(taskId)
                    .orElseThrow(() -> new NotFoundException("护理任务不存在"));
            permissionService.assertCanAccessElder(currentUser, task.getElderId());
            if ("draft".equalsIgnoreCase(task.getStatus())) {
                draftTasks.add(task);
            }
        }

        ConfirmCarePlanTasksResponse response = new ConfirmCarePlanTasksResponse();
        if (draftTasks.isEmpty()) {
            response.setConfirmedTaskCount(0);
            response.setMessage("选中的任务中没有可确认的 draft 任务");
            return response;
        }
        Long elderId = draftTasks.get(0).getElderId();
        autoAssignTasksBySchedule(draftTasks, elderId);
        boolean hasUnassigned = draftTasks.stream().anyMatch(task -> task.getAssignedNurseId() == null);
        if (hasUnassigned) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "存在任务未绑定护理人员，请先分配护理人员后再确认", HttpStatus.BAD_REQUEST);
        }
        LocalDateTime now = LocalDateTime.now();
        for (CarePlanTask task : draftTasks) {
            task.setStatus("pending");
            task.setUpdatedAt(now);
        }
        carePlanTaskRepository.saveAll(draftTasks);
        Long carePlanId = draftTasks.get(0).getCarePlanId();
        createTaskNotifications(draftTasks, carePlanId);
        response.setCarePlanId(carePlanId);
        response.setConfirmedTaskCount(draftTasks.size());
        response.setMessage("护理任务已确认分配");
        return response;
    }

    @Transactional
    public List<CarePlanTaskDTO> batchUpdateTaskTime(CurrentUser currentUser, BatchUpdateCarePlanTaskTimeRequest request) {
        requireTaskConfirmer(currentUser);
        List<CarePlanTaskDTO> result = new ArrayList<>();
        for (Long taskId : request.getTaskIds()) {
            if (taskId == null) {
                continue;
            }
            CarePlanTask task = carePlanTaskRepository.findById(taskId)
                    .orElseThrow(() -> new NotFoundException("护理任务不存在"));
            permissionService.assertCanAccessElder(currentUser, task.getElderId());
            String status = safe(task.getStatus()).toLowerCase(Locale.ROOT);
            if ("completed".equals(status)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "已完成任务不允许批量修改执行时间", HttpStatus.BAD_REQUEST);
            }
            if ("cancelled".equals(status)) {
                continue;
            }
            applyTaskSchedule(task, request.getScheduledDate(), request.getScheduledTime(), null);
            task.setUpdatedAt(LocalDateTime.now());
            result.add(CarePlanTaskDTO.from(carePlanTaskRepository.save(task)));
        }
        return result;
    }

    @Transactional
    public List<CarePlanTaskDTO> batchUpdateTaskAssignee(CurrentUser currentUser, BatchUpdateCarePlanTaskAssigneeRequest request) {
        requireTaskConfirmer(currentUser);
        List<CarePlanTaskDTO> result = new ArrayList<>();
        for (Long taskId : request.getTaskIds()) {
            if (taskId == null) {
                continue;
            }
            CarePlanTask task = carePlanTaskRepository.findById(taskId)
                    .orElseThrow(() -> new NotFoundException("护理任务不存在"));
            permissionService.assertCanAccessElder(currentUser, task.getElderId());
            String status = safe(task.getStatus()).toLowerCase(Locale.ROOT);
            if ("completed".equals(status)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "已完成任务不允许批量修改护理人员", HttpStatus.BAD_REQUEST);
            }
            if ("cancelled".equals(status)) {
                continue;
            }
            task.setAssignedNurseId(request.getAssignedNurseId());
            task.setUpdatedAt(LocalDateTime.now());
            result.add(CarePlanTaskDTO.from(carePlanTaskRepository.save(task)));
        }
        return result;
    }

    @Transactional
    public AutoAssignCarePlanTasksResponse autoAssignTasks(CurrentUser currentUser, Long carePlanId) {
        requireTaskConfirmer(currentUser);
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());
        List<CarePlanTask> tasks = carePlanTaskRepository
                .findAllByCarePlanIdAndStatusInOrderByScheduledAtAscCreatedAtAscTaskIdAsc(carePlanId, List.of("draft", "pending"));
        AutoAssignCarePlanTasksResponse response = new AutoAssignCarePlanTasksResponse();
        response.setCarePlanId(carePlanId);
        if (tasks.isEmpty()) {
            response.setAssignedCount(0);
            response.setUnassignedCount(0);
            response.setMessage("当前护理计划没有可自动分配的 draft/pending 任务");
            return response;
        }
        AssignmentSummary summary = autoAssignTasksBySchedule(tasks, plan.getElderId());
        carePlanTaskRepository.saveAll(tasks);
        response.setAssignedCount(summary.assignedCount());
        response.setUnassignedCount(summary.unassignedCount());
        response.setMessage(summary.unassignedCount() > 0
                ? "部分任务未匹配到排班护理人员，请手动分配"
                : "护理任务已根据排班自动分配");
        return response;
    }

    @Transactional
    public ConfirmCarePlanTasksResponse confirmTasks(CurrentUser currentUser, Long carePlanId) {
        requireTaskConfirmer(currentUser);
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());

        List<CarePlanTask> draftTasks = carePlanTaskRepository.findAllByCarePlanIdAndStatusOrderByScheduledAtAscCreatedAtAscTaskIdAsc(carePlanId, "draft");
        ConfirmCarePlanTasksResponse response = new ConfirmCarePlanTasksResponse();
        response.setCarePlanId(carePlanId);
        if (draftTasks.isEmpty()) {
            response.setConfirmedTaskCount(0);
            response.setMessage("当前护理计划没有待确认的护理任务");
            return response;
        }

        autoAssignTasksBySchedule(draftTasks, plan.getElderId());
        boolean hasUnassigned = draftTasks.stream().anyMatch(task -> task.getAssignedNurseId() == null);
        if (hasUnassigned) {
            response.setConfirmedTaskCount(0);
            response.setMessage("存在未分配护理人员的任务，请先自动分配或手动指定护理人员");
            return response;
        }

        LocalDateTime now = LocalDateTime.now();
        for (CarePlanTask task : draftTasks) {
            task.setStatus("pending");
            task.setUpdatedAt(now);
        }
        carePlanTaskRepository.saveAll(draftTasks);
        createTaskNotifications(draftTasks, carePlanId);
        response.setConfirmedTaskCount(draftTasks.size());
        response.setMessage("护理任务已确认分配");
        return response;
    }

    @Transactional
    public void softDeleteTasksByCarePlanId(Long carePlanId) {
        if (carePlanId == null) {
            return;
        }
        List<CarePlanTask> tasks = carePlanTaskRepository.findAllByCarePlanIdOrderByScheduledAtAscCreatedAtAscTaskIdAsc(carePlanId);
        if (tasks.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (CarePlanTask task : tasks) {
            task.setStatus("cancelled");
            if (!StringUtils.hasText(task.getExecutionResult())) {
                task.setExecutionResult("随护理计划删除自动取消");
            }
            task.setUpdatedAt(now);
        }
        carePlanTaskRepository.saveAll(tasks);
    }

    private int softDeleteTask(CarePlanTask task) {
        if (task == null) {
            return 0;
        }
        if ("completed".equalsIgnoreCase(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已完成的护理任务不允许删除", HttpStatus.BAD_REQUEST);
        }
        if ("cancelled".equalsIgnoreCase(task.getStatus())) {
            return 0;
        }
        task.setStatus("cancelled");
        if (!StringUtils.hasText(task.getExecutionResult())) {
            task.setExecutionResult("任务已删除");
        }
        task.setUpdatedAt(LocalDateTime.now());
        carePlanTaskRepository.save(task);
        return 1;
    }

    private String buildDeleteMessage(int deletedCount) {
        return deletedCount > 0 ? "护理任务删除成功" : "没有可删除的护理任务";
    }

    private List<Long> resolveAssignedNurseIds(Long elderId) {
        return careTeamAssignmentRepository.findActiveNurseIdsByElderId(elderId)
                .stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private List<CarePlanTask> expandDraftTasksForNurses(List<CarePlanTask> draftTasks, List<Long> assignedNurseIds) {
        if (draftTasks == null || draftTasks.isEmpty()) {
            return List.of();
        }
        if (assignedNurseIds == null || assignedNurseIds.isEmpty()) {
            return draftTasks;
        }

        List<CarePlanTask> expanded = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (CarePlanTask task : draftTasks) {
            if (task.getAssignedNurseId() != null) {
                expanded.add(task);
                continue;
            }
            boolean first = true;
            for (Long assignedNurseId : assignedNurseIds) {
                if (first) {
                    task.setAssignedNurseId(assignedNurseId);
                    task.setUpdatedAt(now);
                    expanded.add(task);
                    first = false;
                } else {
                    expanded.add(cloneTask(task, assignedNurseId, now));
                }
            }
        }
        return expanded;
    }

    private CarePlanTask buildTaskEntity(CarePlan plan,
                                         Long assignedNurseId,
                                         CarePlanTaskDraft draft,
                                         LocalDateTime now) {
        CarePlanTask task = new CarePlanTask();
        task.setCarePlanId(plan.getCarePlanId());
        task.setElderId(plan.getElderId());
        task.setAssignedNurseId(assignedNurseId);
        task.setTaskType(draft.taskType);
        task.setTaskTitle(draft.taskTitle);
        task.setTaskContent(draft.taskContent);
        task.setFrequencyDesc(draft.frequencyDesc);
        task.setSuggestedTime(draft.suggestedTime);
        task.setScheduledDate(draft.scheduledDate);
        task.setScheduledTime(draft.scheduledTime);
        task.setScheduledAt(draft.scheduledAt);
        task.setTaskSource(draft.taskSource);
        task.setTaskGroupKey(draft.taskGroupKey);
        task.setStatus("draft");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }

    private CarePlanTask cloneTask(CarePlanTask source, Long assignedNurseId, LocalDateTime now) {
        CarePlanTask cloned = new CarePlanTask();
        cloned.setCarePlanId(source.getCarePlanId());
        cloned.setElderId(source.getElderId());
        cloned.setAssignedNurseId(assignedNurseId);
        cloned.setTaskType(source.getTaskType());
        cloned.setTaskTitle(source.getTaskTitle());
        cloned.setTaskContent(source.getTaskContent());
        cloned.setFrequencyDesc(source.getFrequencyDesc());
        cloned.setSuggestedTime(source.getSuggestedTime());
        cloned.setScheduledDate(source.getScheduledDate());
        cloned.setScheduledTime(source.getScheduledTime());
        cloned.setScheduledAt(source.getScheduledAt());
        cloned.setTaskSource(source.getTaskSource());
        cloned.setTaskGroupKey(source.getTaskGroupKey());
        cloned.setStatus(source.getStatus());
        cloned.setExecutionResult(source.getExecutionResult());
        cloned.setCreatedAt(now);
        cloned.setUpdatedAt(now);
        return cloned;
    }

    private TaskGenerationOutcome generateTasksInternal(CarePlan plan, boolean notifyFailureWithBusinessError) {
        if (!isAutoGeneratableStatus(plan.getStatus())) {
            return TaskGenerationOutcome.skipped(plan.getCarePlanId(), "护理计划状态不是 active，跳过任务生成");
        }
        if (carePlanTaskRepository.existsByCarePlanId(plan.getCarePlanId())) {
            List<CarePlanTask> existingTasks = carePlanTaskRepository.findAllByCarePlanIdOrderByScheduledAtAscCreatedAtAscTaskIdAsc(plan.getCarePlanId());
            boolean hasOnlyDraft = existingTasks.stream().allMatch(task -> "draft".equalsIgnoreCase(task.getStatus()));
            if (hasOnlyDraft) {
                return TaskGenerationOutcome.skipped(plan.getCarePlanId(), "该护理计划已存在待确认任务，请先确认或取消后再重新生成");
            }
            return TaskGenerationOutcome.skipped(plan.getCarePlanId(), "该护理计划已存在护理任务，跳过自动生成");
        }

        List<CarePlanTaskDraft> drafts = buildTaskDrafts(plan);
        if (drafts.isEmpty()) {
            return TaskGenerationOutcome.skipped(plan.getCarePlanId(), "当前护理计划缺少可拆分的护理任务内容");
        }

        LocalDateTime now = LocalDateTime.now();
        List<CarePlanTask> tasks = new ArrayList<>();
        Set<String> dedupKeys = new LinkedHashSet<>();
        for (CarePlanTaskDraft draft : drafts) {
            String dedupKey = buildScheduledDedupKey(null, draft);
            if (dedupKeys.add(dedupKey)) {
                tasks.add(buildTaskEntity(plan, null, draft, now));
            }
        }
        AssignmentSummary assignmentSummary = autoAssignTasksBySchedule(tasks, plan.getElderId());
        carePlanTaskRepository.saveAll(tasks);
        if (assignmentSummary.assignedCount() == 0) {
            return TaskGenerationOutcome.generated(plan.getCarePlanId(), tasks.size(), "护理任务草稿已生成，但未匹配到排班护理人员，请先自动分配或手动指定");
        }
        if (assignmentSummary.unassignedCount() > 0) {
            return TaskGenerationOutcome.generated(plan.getCarePlanId(), tasks.size(), "护理任务草稿已生成，部分任务未匹配到排班护理人员");
        }
        return TaskGenerationOutcome.generated(plan.getCarePlanId(), tasks.size(), "护理任务草稿已生成，并已根据排班推荐护理人员");
    }

    private GenerateCarePlanTasksResponse toResponse(TaskGenerationOutcome outcome) {
        GenerateCarePlanTasksResponse response = new GenerateCarePlanTasksResponse();
        response.setCarePlanId(outcome.getCarePlanId());
        response.setTaskGenerated(outcome.isTaskGenerated());
        response.setGeneratedCount(outcome.getGeneratedTaskCount());
        response.setTaskGenerateMessage(outcome.getTaskGenerateMessage());
        return response;
    }

    private AssignmentSummary autoAssignTasksBySchedule(List<CarePlanTask> tasks, Long elderId) {
        if (tasks == null || tasks.isEmpty()) {
            return new AssignmentSummary(0, 0);
        }
        List<Long> candidateIds = resolveAssignedNurseIds(elderId);
        if (candidateIds.isEmpty()) {
            tasks.forEach(task -> task.setAssignedNurseId(null));
            return new AssignmentSummary(0, tasks.size());
        }

        Map<Long, User> candidateUserMap = new HashMap<>();
        userRepository.findAllById(candidateIds).forEach(user -> candidateUserMap.put(user.getUserId(), user));
        List<Long> availableCandidateIds = candidateIds.stream()
                .filter(candidateUserMap::containsKey)
                .toList();
        if (availableCandidateIds.isEmpty()) {
            tasks.forEach(task -> task.setAssignedNurseId(null));
            return new AssignmentSummary(0, tasks.size());
        }

        List<LocalDate> scheduledDates = tasks.stream()
                .map(CarePlanTask::getScheduledDate)
                .filter(date -> date != null)
                .distinct()
                .toList();
        Map<String, Integer> loadMap = buildDailyTaskLoadMap(availableCandidateIds, scheduledDates, tasks);
        Map<Long, List<StaffShiftSchedule>> shiftMap = buildShiftMap(availableCandidateIds, scheduledDates);

        int assignedCount = 0;
        int unassignedCount = 0;
        for (CarePlanTask task : tasks) {
            Long assignedId = pickBestAssignee(task, availableCandidateIds, candidateUserMap, shiftMap, loadMap);
            task.setAssignedNurseId(assignedId);
            if (assignedId == null) {
                unassignedCount += 1;
                continue;
            }
            assignedCount += 1;
            if (task.getScheduledDate() != null) {
                String key = buildLoadKey(assignedId, task.getScheduledDate());
                loadMap.put(key, loadMap.getOrDefault(key, 0) + 1);
            }
        }
        return new AssignmentSummary(assignedCount, unassignedCount);
    }

    private Map<String, Integer> buildDailyTaskLoadMap(List<Long> candidateIds,
                                                       List<LocalDate> scheduledDates,
                                                       List<CarePlanTask> targetTasks) {
        Map<String, Integer> loadMap = new HashMap<>();
        if (candidateIds.isEmpty() || scheduledDates.isEmpty()) {
            return loadMap;
        }
        Set<Long> targetTaskIds = targetTasks.stream()
                .map(CarePlanTask::getTaskId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        List<CarePlanTask> existingTasks = carePlanTaskRepository
                .findAllByAssignedNurseIdInAndScheduledDateInAndStatusNotIn(candidateIds, scheduledDates, List.of("cancelled"));
        for (CarePlanTask existingTask : existingTasks) {
            if (existingTask.getTaskId() != null && targetTaskIds.contains(existingTask.getTaskId())) {
                continue;
            }
            if (existingTask.getAssignedNurseId() == null || existingTask.getScheduledDate() == null) {
                continue;
            }
            String key = buildLoadKey(existingTask.getAssignedNurseId(), existingTask.getScheduledDate());
            loadMap.put(key, loadMap.getOrDefault(key, 0) + 1);
        }
        return loadMap;
    }

    private Map<Long, List<StaffShiftSchedule>> buildShiftMap(List<Long> candidateIds, List<LocalDate> scheduledDates) {
        Map<Long, List<StaffShiftSchedule>> shiftMap = new HashMap<>();
        if (candidateIds.isEmpty() || scheduledDates.isEmpty()) {
            return shiftMap;
        }
        List<StaffShiftSchedule> shifts = staffShiftScheduleRepository
                .findAllByStaffIdInAndShiftDateInAndStatusOrderByShiftDateAscStartTimeAsc(candidateIds, scheduledDates, "active");
        for (StaffShiftSchedule shift : shifts) {
            shiftMap.computeIfAbsent(shift.getStaffId(), key -> new ArrayList<>()).add(shift);
        }
        return shiftMap;
    }

    private Long pickBestAssignee(CarePlanTask task,
                                  List<Long> candidateIds,
                                  Map<Long, User> candidateUserMap,
                                  Map<Long, List<StaffShiftSchedule>> shiftMap,
                                  Map<String, Integer> loadMap) {
        List<Long> preferredCandidateIds = filterCandidatesByTaskType(candidateIds, candidateUserMap, task.getTaskType());
        List<Long> shiftMatchedCandidateIds = filterCandidatesByShift(preferredCandidateIds, task, shiftMap);
        List<Long> finalCandidates = shiftMatchedCandidateIds.isEmpty() ? preferredCandidateIds : shiftMatchedCandidateIds;
        if (finalCandidates.isEmpty()) {
            return null;
        }
        return finalCandidates.stream()
                .min(Comparator
                        .comparingInt((Long staffId) -> currentDailyTaskLoad(loadMap, staffId, task.getScheduledDate()))
                        .thenComparingLong(Long::longValue))
                .orElse(null);
    }

    private List<Long> filterCandidatesByTaskType(List<Long> candidateIds,
                                                  Map<Long, User> candidateUserMap,
                                                  String taskType) {
        String taskTypeCode = normalizeTaskTypeCode(taskType);
        List<Long> preferred = candidateIds.stream()
                .filter(staffId -> roleMatchesTask(taskTypeCode, normalizeRole(candidateUserMap.get(staffId))))
                .toList();
        return preferred.isEmpty() ? candidateIds : preferred;
    }

    private List<Long> filterCandidatesByShift(List<Long> candidateIds,
                                               CarePlanTask task,
                                               Map<Long, List<StaffShiftSchedule>> shiftMap) {
        if (task.getScheduledDate() == null) {
            return List.of();
        }
        return candidateIds.stream()
                .filter(staffId -> hasMatchingShift(shiftMap.get(staffId), task.getScheduledDate(), task.getScheduledTime()))
                .toList();
    }

    private boolean hasMatchingShift(List<StaffShiftSchedule> shifts, LocalDate scheduledDate, LocalTime scheduledTime) {
        if (shifts == null || shifts.isEmpty() || scheduledDate == null) {
            return false;
        }
        for (StaffShiftSchedule shift : shifts) {
            if (!scheduledDate.equals(shift.getShiftDate())) {
                continue;
            }
            if (scheduledTime == null) {
                return true;
            }
            boolean within = !scheduledTime.isBefore(shift.getStartTime()) && !scheduledTime.isAfter(shift.getEndTime());
            if (within) {
                return true;
            }
        }
        return false;
    }

    private boolean roleMatchesTask(String taskTypeCode, String role) {
        if (!StringUtils.hasText(role)) {
            return false;
        }
        return switch (taskTypeCode) {
            case "health_monitoring", "medication_care", "evaluation" -> "nurse".equals(role);
            case "daily_care", "diet_plan", "diet_care", "safety_precaution", "safety" -> "caregiver".equals(role);
            case "rehabilitation_activity", "rehabilitation" -> "caregiver".equals(role) || "nurse".equals(role);
            case "psychological_care", "psychological" -> "caregiver".equals(role) || "nurse".equals(role);
            default -> "nurse".equals(role) || "caregiver".equals(role);
        };
    }

    private String normalizeRole(User user) {
        return user == null ? "" : normalizeRole(user.getRole());
    }

    private String normalizeRole(String role) {
        return safe(role).toLowerCase(Locale.ROOT);
    }

    private int currentDailyTaskLoad(Map<String, Integer> loadMap, Long staffId, LocalDate date) {
        if (staffId == null || date == null) {
            return 0;
        }
        return loadMap.getOrDefault(buildLoadKey(staffId, date), 0);
    }

    private String buildLoadKey(Long staffId, LocalDate date) {
        return safe(staffId == null ? "" : staffId.toString()) + "|" + safe(date == null ? "" : date.toString());
    }

    public void createOverdueNotifications() {
        List<CarePlanTask> overdueTasks = carePlanTaskRepository
                .findAllByStatusIgnoreCaseOrderByScheduledAtAscCreatedAtAscTaskIdAsc("pending")
                .stream()
                .filter(this::isTaskOverdue)
                .filter(task -> task.getAssignedNurseId() != null)
                .toList();
        for (CarePlanTask task : overdueTasks) {
            Long receiverId = task.getAssignedNurseId();
            if (receiverId == null) {
                continue;
            }
            boolean exists = notificationRepositoryExists(receiverId, "care_task_overdue", "care_plan_task", task.getTaskId());
            if (exists) {
                continue;
            }
            try {
                String username = userRepository.findById(receiverId)
                        .map(User::getUsername)
                        .orElse(null);
                notificationService.createSystemNotification(
                        receiverId,
                        username,
                        "护理任务逾期提醒",
                        "您有护理任务已超过计划执行时间，请及时处理。",
                        "care_task_overdue",
                        "care_plan_task",
                        task.getTaskId()
                );
            } catch (Exception ex) {
                log.warn("创建护理任务逾期提醒失败，taskId={}, receiverId={}, error={}", task.getTaskId(), receiverId, ex.getMessage());
            }
        }
    }

    private boolean notificationRepositoryExists(Long toUserId, String notifType, String bizType, Long bizId) {
        try {
            return notificationRepository.existsByToUserIdAndNotifTypeAndBizTypeAndBizId(toUserId, notifType, bizType, bizId);
        } catch (Exception ex) {
            log.warn("检查逾期通知是否已存在失败，receiverId={}, bizId={}, error={}", toUserId, bizId, ex.getMessage());
            return false;
        }
    }

    private List<CarePlanTask> resolveTaskScope(CurrentUser currentUser,
                                                Long carePlanId,
                                                Long elderId,
                                                Long assignedNurseId) {
        List<CarePlanTask> baseTasks;
        if (carePlanId != null) {
            CarePlan plan = carePlanRepository.findById(carePlanId)
                    .orElseThrow(() -> new NotFoundException("护理计划不存在"));
            permissionService.assertCanAccessElder(currentUser, plan.getElderId());
            baseTasks = carePlanTaskRepository.findAllByCarePlanIdOrderByScheduledAtAscCreatedAtAscTaskIdAsc(carePlanId);
        } else if (isTaskManager(currentUser)) {
            baseTasks = carePlanTaskRepository.findAll();
        } else {
            baseTasks = carePlanTaskRepository.findAllByAssignedNurseIdAndStatusNotInOrderByScheduledAtAscCreatedAtAscTaskIdAsc(
                    currentUser.getUserId(),
                    List.of("draft"));
        }
        return filterAccessibleTasks(currentUser, baseTasks).stream()
                .filter(task -> elderId == null || elderId.equals(task.getElderId()))
                .filter(task -> assignedNurseId == null || assignedNurseId.equals(task.getAssignedNurseId()))
                .toList();
    }

    private List<CarePlanTask> filterAccessibleTasks(CurrentUser currentUser, List<CarePlanTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        if (isTaskManager(currentUser)) {
            return tasks;
        }
        return tasks.stream()
                .filter(task -> currentUser.getUserId().equals(task.getAssignedNurseId()))
                .toList();
    }

    private boolean isTaskManager(CurrentUser currentUser) {
        return currentUser != null && (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader"));
    }

    private boolean isTaskOverdue(CarePlanTask task) {
        if (task == null || !"pending".equalsIgnoreCase(task.getStatus())) {
            return false;
        }
        LocalDateTime scheduledAt = resolveTaskScheduledAt(task);
        return scheduledAt != null && scheduledAt.isBefore(LocalDateTime.now());
    }

    private boolean isCancelled(String status) {
        String normalized = safe(status).toLowerCase(Locale.ROOT);
        return "cancelled".equals(normalized) || "canceled".equals(normalized);
    }

    private LocalDate resolveTaskDate(CarePlanTask task) {
        if (task == null) {
            return LocalDate.of(9999, 12, 31);
        }
        if (task.getScheduledDate() != null) {
            return task.getScheduledDate();
        }
        if (task.getScheduledAt() != null) {
            return task.getScheduledAt().toLocalDate();
        }
        return task.getCreatedAt() == null ? LocalDate.of(9999, 12, 31) : task.getCreatedAt().toLocalDate();
    }

    private LocalDateTime resolveTaskScheduledAt(CarePlanTask task) {
        if (task == null) {
            return null;
        }
        if (task.getScheduledAt() != null) {
            return task.getScheduledAt();
        }
        if (task.getScheduledDate() != null) {
            return LocalDateTime.of(task.getScheduledDate(), task.getScheduledTime() == null ? LocalTime.MAX : task.getScheduledTime());
        }
        return task.getCreatedAt();
    }

    private double calculateCompletionRate(List<CarePlanTask> tasks) {
        long executableCount = tasks.stream()
                .filter(task -> !"draft".equalsIgnoreCase(task.getStatus()))
                .count();
        if (executableCount <= 0) {
            return 0D;
        }
        long completedCount = tasks.stream()
                .filter(task -> "completed".equalsIgnoreCase(task.getStatus()))
                .count();
        return Math.round((completedCount * 10000.0d) / executableCount) / 100.0d;
    }

    private List<CarePlanExecutionReportResponse.TypeSummary> buildTypeSummaries(List<CarePlanTask> tasks) {
        Map<String, List<CarePlanTask>> grouped = tasks.stream()
                .collect(Collectors.groupingBy(task -> safe(task.getTaskType())));
        return grouped.entrySet().stream()
                .map(entry -> {
                    CarePlanExecutionReportResponse.TypeSummary item = new CarePlanExecutionReportResponse.TypeSummary();
                    item.setTaskType(entry.getKey());
                    item.setTaskTypeLabel(normalizeTaskType(entry.getKey()));
                    item.setTotalCount(entry.getValue().size());
                    item.setCompletedCount(entry.getValue().stream().filter(task -> "completed".equalsIgnoreCase(task.getStatus())).count());
                    item.setPendingCount(entry.getValue().stream().filter(task -> "pending".equalsIgnoreCase(task.getStatus())).count());
                    item.setOverdueCount(entry.getValue().stream().filter(this::isTaskOverdue).count());
                    return item;
                })
                .sorted(Comparator.comparing(CarePlanExecutionReportResponse.TypeSummary::getTaskTypeLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<CarePlanExecutionReportResponse.DateSummary> buildDateSummaries(List<CarePlanTask> tasks) {
        Map<LocalDate, List<CarePlanTask>> grouped = tasks.stream()
                .collect(Collectors.groupingBy(this::resolveTaskDate));
        return grouped.entrySet().stream()
                .map(entry -> {
                    CarePlanExecutionReportResponse.DateSummary item = new CarePlanExecutionReportResponse.DateSummary();
                    item.setDate(entry.getKey());
                    item.setTotalCount(entry.getValue().size());
                    item.setCompletedCount(entry.getValue().stream().filter(task -> "completed".equalsIgnoreCase(task.getStatus())).count());
                    item.setPendingCount(entry.getValue().stream().filter(task -> "pending".equalsIgnoreCase(task.getStatus())).count());
                    item.setOverdueCount(entry.getValue().stream().filter(this::isTaskOverdue).count());
                    item.setCancelledCount(entry.getValue().stream().filter(task -> isCancelled(task.getStatus())).count());
                    return item;
                })
                .sorted(Comparator.comparing(CarePlanExecutionReportResponse.DateSummary::getDate))
                .toList();
    }

    private List<CarePlanExecutionReportResponse.ExecutionRecord> buildRecentExecutionRecords(List<CarePlanTask> tasks) {
        return tasks.stream()
                .filter(task -> task.getExecutedAt() != null)
                .sorted(Comparator.comparing(CarePlanTask::getExecutedAt).reversed())
                .limit(10)
                .map(task -> {
                    CarePlanExecutionReportResponse.ExecutionRecord item = new CarePlanExecutionReportResponse.ExecutionRecord();
                    item.setTaskId(task.getTaskId());
                    item.setTaskTitle(task.getTaskTitle());
                    item.setTaskType(task.getTaskType());
                    item.setTaskTypeLabel(normalizeTaskType(task.getTaskType()));
                    item.setExecutionResult(task.getExecutionResult());
                    item.setExecutedAt(task.getExecutedAt());
                    item.setStatus(task.getStatus());
                    return item;
                })
                .toList();
    }

    private String buildExecutionSummaryText(CarePlanExecutionReportResponse response) {
        long total = response.getTotalTaskCount();
        long completed = response.getCompletedTaskCount();
        double rate = response.getCompletionRate();
        long overdue = response.getOverdueTaskCount();
        String bestTypeText = response.getTasksByType().stream()
                .max(Comparator.comparingLong(CarePlanExecutionReportResponse.TypeSummary::getCompletedCount))
                .map(CarePlanExecutionReportResponse.TypeSummary::getTaskTypeLabel)
                .filter(StringUtils::hasText)
                .orElse("护理任务");
        if (overdue > 0) {
            return String.format(Locale.CHINA,
                    "该护理计划共生成 %d 条护理任务，已完成 %d 条，完成率 %.2f%%。其中%s任务完成情况较好，仍有 %d 条任务逾期未完成，建议护理人员及时跟进。",
                    total,
                    completed,
                    rate,
                    bestTypeText,
                    overdue);
        }
        return String.format(Locale.CHINA,
                "该护理计划共生成 %d 条护理任务，已完成 %d 条，完成率 %.2f%%。其中%s任务完成情况较好，当前暂无逾期任务。",
                total,
                completed,
                rate,
                bestTypeText);
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(user -> firstNonBlank(user.getRealName(), user.getUsername()))
                .orElse(null);
    }

    private void createTaskNotifications(List<CarePlanTask> tasks, Long carePlanId) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Set<Long> notifiedUserIds = new HashSet<>();
        for (CarePlanTask task : tasks) {
            Long receiverId = task.getAssignedNurseId();
            if (receiverId == null || !notifiedUserIds.add(receiverId)) {
                continue;
            }
            try {
                String username = userRepository.findById(receiverId)
                        .map(user -> user.getUsername())
                        .orElse(null);
                notificationService.createSystemNotification(
                        receiverId,
                        username,
                        "新的护理任务",
                        "老人护理计划已生成新的护理任务，请及时查看并执行。",
                        "care_task",
                        "care_plan",
                        carePlanId
                );
            } catch (Exception ex) {
                log.warn("创建护理任务通知失败，receiverId={}, carePlanId={}, error={}", receiverId, carePlanId, ex.getMessage());
            }
        }
    }

    private void requireTaskManager(CurrentUser currentUser) {
        if (!(currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader"))) {
            throw new AccessDeniedException("仅管理员/护士长可生成护理任务");
        }
    }

    private void requireTaskExecutor(CurrentUser currentUser) {
        if (!(currentUser.hasRole("admin")
                || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("nurse")
                || currentUser.hasRole("caregiver"))) {
            throw new AccessDeniedException("当前角色无权限查看护理任务");
        }
    }

    private void requireTaskConfirmer(CurrentUser currentUser) {
        if (currentUser.hasRole("admin")
                || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("nurse")
                || currentUser.hasRole("caregiver")) {
            return;
        }
        throw new AccessDeniedException("当前角色无权限确认分配护理任务");
    }

    private List<CarePlanTaskDraft> buildTaskDrafts(CarePlan plan) {
        List<CarePlanTaskDraft> aiDrafts = buildTaskDraftsWithAi(plan);
        return aiDrafts.isEmpty() ? buildTaskDraftsByRule(plan) : aiDrafts;
    }

    private List<CarePlanTaskDraft> buildTaskDraftsWithAi(CarePlan plan) {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            return List.of();
        }
        try {
            String content = aiClient.chat(buildSystemPrompt(), buildUserPrompt(plan));
            return parseAiTaskDrafts(stripCodeFence(content), plan);
        } catch (Exception ex) {
            log.warn("AI拆分护理任务失败，使用规则兜底: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<CarePlanTaskDraft> parseAiTaskDrafts(String cleaned, CarePlan plan) throws IOException {
        JsonNode root = objectMapper.readTree(cleaned);
        List<CarePlanTaskDraft> drafts = new ArrayList<>();
        if (!root.isArray()) {
            return drafts;
        }
        for (JsonNode node : root) {
            CarePlanTaskDraft draft = new CarePlanTaskDraft();
            draft.taskTypeCode = normalizeTaskTypeCode(node.path("taskType").asText(""));
            draft.taskType = normalizeTaskType(node.path("taskType").asText(""));
            draft.taskTitle = safe(node.path("taskTitle").asText(""));
            draft.taskContent = safe(node.path("taskContent").asText(""));
            draft.frequencyDesc = firstNonBlank(safe(node.path("frequencyDesc").asText("")), resolveFrequencyForTask(plan, draft.taskTypeCode));
            draft.suggestedTime = resolveSuggestedTime(
                    safe(node.path("suggestedTime").asText("")),
                    plan.getCareTime(),
                    draft.taskTypeCode);
            draft.taskSource = "care_plan";
            if (StringUtils.hasText(draft.taskTitle) && StringUtils.hasText(draft.taskContent)) {
                draft.taskGroupKey = buildTaskGroupKey(plan.getCarePlanId(), draft.taskTypeCode, draft.taskTitle);
                addExpandedDrafts(drafts, draft, plan);
            }
        }
        return deduplicateDrafts(drafts);
    }

    private List<CarePlanTaskDraft> buildTaskDraftsByRule(CarePlan plan) {
        List<CarePlanTaskDraft> drafts = new ArrayList<>();
        addDraftIfPresent(drafts, "health_monitoring", "健康监测任务", plan.getHealthMonitoring(), plan);
        addDraftIfPresent(drafts, "daily_care", "生活护理任务", firstNonBlank(plan.getDailyCare(), plan.getCareContent()), plan);
        addDraftIfPresent(drafts, "diet_plan", "饮食护理任务", plan.getDietPlan(), plan);
        addDraftIfPresent(drafts, "medication_care", "用药护理任务", firstNonBlank(plan.getMedicationCare(), plan.getMedicationReminder()), plan);
        addDraftIfPresent(drafts, "rehabilitation_activity", "康复活动任务", plan.getRehabilitationActivity(), plan);
        addDraftIfPresent(drafts, "psychological_care", "心理关怀任务", plan.getPsychologicalCare(), plan);
        addDraftIfPresent(drafts, "safety_precaution", "安全防护任务", plan.getSafetyPrecaution(), plan);
        addDraftIfPresent(drafts, "evaluation", "护理评价任务", plan.getEvaluation(), plan);
        return deduplicateDrafts(drafts);
    }

    private void addDraftIfPresent(List<CarePlanTaskDraft> drafts,
                                   String taskTypeCode,
                                   String fallbackTitle,
                                   String taskContent,
                                   CarePlan plan) {
        if (!StringUtils.hasText(taskContent)) {
            return;
        }
        CarePlanTaskDraft draft = new CarePlanTaskDraft();
        draft.taskTypeCode = taskTypeCode;
        draft.taskType = normalizeTaskType(taskTypeCode);
        draft.taskTitle = resolveDraftTitle(fallbackTitle, taskContent);
        draft.taskContent = taskContent.trim();
        draft.frequencyDesc = resolveFrequencyForTask(plan, taskTypeCode);
        draft.suggestedTime = resolveSuggestedTime("", plan.getCareTime(), taskTypeCode);
        draft.taskSource = "care_plan";
        draft.taskGroupKey = buildTaskGroupKey(plan.getCarePlanId(), draft.taskTypeCode, draft.taskTitle);
        addExpandedDrafts(drafts, draft, plan);
    }

    private List<CarePlanTaskDraft> deduplicateDrafts(List<CarePlanTaskDraft> drafts) {
        Map<String, CarePlanTaskDraft> deduped = new LinkedHashMap<>();
        for (CarePlanTaskDraft draft : drafts) {
            String key = (safe(draft.taskTypeCode) + "|" + safe(draft.taskTitle) + "|" + safe(draft.taskContent)
                    + "|" + safe(draft.scheduledAt == null ? "" : draft.scheduledAt.toString())).toLowerCase(Locale.ROOT);
            deduped.putIfAbsent(key, draft);
        }
        return new ArrayList<>(deduped.values());
    }

    private String resolveFrequency(CarePlan plan) {
        return firstNonBlank(plan.getExecutionFrequency(), plan.getCareTime());
    }

    private String resolveFrequencyForTask(CarePlan plan, String taskTypeCode) {
        String frequency = resolveFrequency(plan);
        if (StringUtils.hasText(frequency)) {
            return frequency;
        }
        if ("daily_care".equals(taskTypeCode)
                || "health_monitoring".equals(taskTypeCode)
                || "medication_care".equals(taskTypeCode)) {
            return "每日一次";
        }
        return "";
    }

    private boolean isAutoGeneratableStatus(String status) {
        return !StringUtils.hasText(status) || "active".equalsIgnoreCase(status.trim());
    }

    private String buildSystemPrompt() {
        return "你是养老院护理计划任务拆分助手。"
                + "请把护理计划拆分成可执行的护理任务 JSON 数组。"
                + "只返回纯 JSON 数组，不要 Markdown，不要解释。"
                + "每条任务只返回这些字段：taskType、taskTitle、taskContent、frequencyDesc、suggestedTime。"
                + "只返回任务模板，不要展开成具体日期，不要返回 startDate/endDate 计算结果。"
                + "其中 taskType 必须强制返回中文，不允许返回英文或代码值。"
                + "taskType 只允许使用这些中文值：健康监测、生活护理、饮食护理、用药护理、康复活动、心理关怀、安全防护、护理评价、其他。"
                + "拆分时优先覆盖：healthMonitoring、dailyCare、dietPlan、medicationCare、rehabilitationActivity、psychologicalCare、safetyPrecaution、evaluation。"
                + "suggestedTime 必须尽量生成明确建议执行时间，例如 08:00、09:30、14:00、20:00。"
                + "如果原护理计划里已经有 careTime 或执行频率，请结合这些信息推断 suggestedTime。"
                + "如果是健康监测、用药护理，优先生成早晚时间；如果是生活护理，优先生成晨间时间；如果是康复活动，优先生成上午或下午时间；如果是心理关怀，优先生成下午时间；如果是护理评价，优先生成晚间时间。"
                + "不要生成空任务，不要编造老人不存在的信息。";
    }

    private String buildUserPrompt(CarePlan plan) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("carePlanId", plan.getCarePlanId());
        payload.put("elderId", plan.getElderId());
        payload.put("startDate", plan.getStartDate());
        payload.put("endDate", plan.getEndDate());
        payload.put("careTime", safe(plan.getCareTime()));
        payload.put("executionFrequency", safe(plan.getExecutionFrequency()));
        payload.put("healthMonitoring", safe(plan.getHealthMonitoring()));
        payload.put("dailyCare", safe(plan.getDailyCare()));
        payload.put("dietPlan", safe(plan.getDietPlan()));
        payload.put("medicationCare", firstNonBlank(plan.getMedicationCare(), plan.getMedicationReminder()));
        payload.put("rehabilitationActivity", safe(plan.getRehabilitationActivity()));
        payload.put("psychologicalCare", safe(plan.getPsychologicalCare()));
        payload.put("safetyPrecaution", safe(plan.getSafetyPrecaution()));
        payload.put("evaluation", safe(plan.getEvaluation()));
        return objectMapper.writeValueAsString(payload);
    }

    private String stripCodeFence(String content) {
        String cleaned = content == null ? "" : content.trim();
        if (cleaned.startsWith("```")) {
            int firstLineEnd = cleaned.indexOf('\n');
            if (firstLineEnd >= 0) {
                cleaned = cleaned.substring(firstLineEnd + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
        }
        return cleaned.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void applyTaskSchedule(CarePlanTask task, LocalDate scheduledDate, LocalTime scheduledTime, LocalDateTime scheduledAt) {
        LocalDate nextDate = scheduledDate;
        LocalTime nextTime = scheduledTime;
        LocalDateTime nextScheduledAt = scheduledAt;

        if (nextScheduledAt == null && nextDate != null && nextTime != null) {
            nextScheduledAt = LocalDateTime.of(nextDate, nextTime);
        }
        if (nextScheduledAt != null) {
            if (nextDate == null) {
                nextDate = nextScheduledAt.toLocalDate();
            }
            if (nextTime == null) {
                nextTime = nextScheduledAt.toLocalTime();
            }
        }

        task.setScheduledDate(nextDate);
        task.setScheduledTime(nextTime);
        task.setScheduledAt(nextScheduledAt);
    }

    private String normalizeMutableStatus(String requestedStatus, String fallbackStatus) {
        String normalized = safe(requestedStatus).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            return fallbackStatus;
        }
        return switch (normalized) {
            case "draft", "pending", "completed", "cancelled" -> normalized;
            case "canceled" -> "cancelled";
            default -> fallbackStatus;
        };
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary.trim() : safe(fallback);
    }

    private String normalizeTaskType(String taskType) {
        String code = normalizeTaskTypeCode(taskType);
        return switch (code) {
            case "health_monitoring" -> "健康监测";
            case "daily_care" -> "生活护理";
            case "diet_plan" -> "饮食护理";
            case "medication_care" -> "用药护理";
            case "rehabilitation_activity" -> "康复活动";
            case "psychological_care" -> "心理关怀";
            case "safety_precaution" -> "安全防护";
            case "evaluation" -> "护理评价";
            case "other" -> "其他";
            default -> safe(taskType);
        };
    }

    private String normalizeTaskTypeCode(String taskType) {
        String value = safe(taskType).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(value)) {
            return "other";
        }
        if (value.contains("健康监测") || value.contains("health")) {
            return "health_monitoring";
        }
        if (value.contains("生活护理") || value.contains("daily")) {
            return "daily_care";
        }
        if (value.contains("饮食护理") || value.contains("diet")) {
            return "diet_plan";
        }
        if (value.contains("用药护理") || value.contains("medication") || value.contains("medicine")) {
            return "medication_care";
        }
        if (value.contains("康复活动") || value.contains("rehabilitation")) {
            return "rehabilitation_activity";
        }
        if (value.contains("心理关怀") || value.contains("psychological")) {
            return "psychological_care";
        }
        if (value.contains("安全防护") || value.contains("safety")) {
            return "safety_precaution";
        }
        if (value.contains("护理评价") || value.contains("evaluation")) {
            return "evaluation";
        }
        if (value.contains("其他") || value.contains("other")) {
            return "other";
        }
        return safe(taskType);
    }

    private String resolveSuggestedTime(String suggestedTime, String careTime, String taskTypeCode) {
        if (StringUtils.hasText(suggestedTime)) {
            return suggestedTime.trim();
        }
        if (StringUtils.hasText(careTime)) {
            return careTime.trim();
        }
        return switch (taskTypeCode) {
            case "health_monitoring" -> "08:00";
            case "medication_care" -> "08:00";
            case "daily_care" -> "09:00";
            case "diet_plan" -> "11:30";
            case "rehabilitation_activity" -> "15:00";
            case "psychological_care" -> "14:00";
            case "safety_precaution" -> "20:00";
            case "evaluation" -> "16:00";
            default -> "09:00";
        };
    }

    private void addExpandedDrafts(List<CarePlanTaskDraft> drafts, CarePlanTaskDraft template, CarePlan plan) {
        List<CarePlanTaskScheduleGenerator.ScheduledTaskSlot> slots = carePlanTaskScheduleGenerator.expand(
                new CarePlanTaskScheduleGenerator.TaskScheduleRequest(
                        resolveStartDate(plan),
                        plan.getEndDate(),
                        template.frequencyDesc,
                        template.suggestedTime,
                        template.taskTypeCode));
        for (CarePlanTaskScheduleGenerator.ScheduledTaskSlot slot : slots) {
            CarePlanTaskDraft scheduledDraft = template.copy();
            scheduledDraft.scheduledDate = slot.scheduledDate();
            scheduledDraft.scheduledTime = slot.scheduledTime();
            scheduledDraft.scheduledAt = slot.scheduledAt();
            drafts.add(scheduledDraft);
        }
    }

    private LocalDate resolveStartDate(CarePlan plan) {
        return plan.getStartDate() == null ? LocalDate.now() : plan.getStartDate();
    }

    private String resolveDraftTitle(String fallbackTitle, String taskContent) {
        if (!StringUtils.hasText(taskContent)) {
            return fallbackTitle;
        }
        String normalized = taskContent.trim()
                .replace('\n', ' ')
                .replace('，', ' ')
                .replace('。', ' ')
                .replace(',', ' ')
                .replace(';', ' ')
                .trim();
        int splitIndex = normalized.indexOf(' ');
        String candidate = splitIndex > 0 ? normalized.substring(0, splitIndex) : normalized;
        if (candidate.length() >= 2 && candidate.length() <= 16) {
            return candidate;
        }
        return fallbackTitle;
    }

    private String buildTaskGroupKey(Long carePlanId, String taskTypeCode, String taskTitle) {
        String raw = safe(carePlanId == null ? "" : carePlanId.toString())
                + "_" + safe(taskTypeCode)
                + "_" + safe(taskTitle).replaceAll("\\s+", "");
        return raw.length() <= 64 ? raw : raw.substring(0, 64);
    }

    private String buildScheduledDedupKey(Long assignedNurseId, CarePlanTaskDraft draft) {
        return safe(assignedNurseId == null ? "" : assignedNurseId.toString())
                + "|" + safe(draft.taskGroupKey)
                + "|" + safe(draft.scheduledAt == null ? "" : draft.scheduledAt.toString());
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static class CarePlanTaskDraft {
        private String taskTypeCode;
        private String taskType;
        private String taskTitle;
        private String taskContent;
        private String frequencyDesc;
        private String suggestedTime;
        private LocalDate scheduledDate;
        private LocalTime scheduledTime;
        private LocalDateTime scheduledAt;
        private String taskSource;
        private String taskGroupKey;

        private CarePlanTaskDraft copy() {
            CarePlanTaskDraft copy = new CarePlanTaskDraft();
            copy.taskTypeCode = this.taskTypeCode;
            copy.taskType = this.taskType;
            copy.taskTitle = this.taskTitle;
            copy.taskContent = this.taskContent;
            copy.frequencyDesc = this.frequencyDesc;
            copy.suggestedTime = this.suggestedTime;
            copy.scheduledDate = this.scheduledDate;
            copy.scheduledTime = this.scheduledTime;
            copy.scheduledAt = this.scheduledAt;
            copy.taskSource = this.taskSource;
            copy.taskGroupKey = this.taskGroupKey;
            return copy;
        }
    }

    private record AssignmentSummary(int assignedCount, int unassignedCount) {
    }

    public static class TaskGenerationOutcome {
        private final Long carePlanId;
        private final boolean taskGenerated;
        private final int generatedTaskCount;
        private final String taskGenerateMessage;

        private TaskGenerationOutcome(Long carePlanId, boolean taskGenerated, int generatedTaskCount, String taskGenerateMessage) {
            this.carePlanId = carePlanId;
            this.taskGenerated = taskGenerated;
            this.generatedTaskCount = generatedTaskCount;
            this.taskGenerateMessage = taskGenerateMessage;
        }

        public static TaskGenerationOutcome generated(Long carePlanId, int generatedTaskCount, String message) {
            return new TaskGenerationOutcome(carePlanId, true, generatedTaskCount, message);
        }

        public static TaskGenerationOutcome skipped(Long carePlanId, String message) {
            return new TaskGenerationOutcome(carePlanId, false, 0, message);
        }

        public Long getCarePlanId() {
            return carePlanId;
        }

        public boolean isTaskGenerated() {
            return taskGenerated;
        }

        public int getGeneratedTaskCount() {
            return generatedTaskCount;
        }

        public String getTaskGenerateMessage() {
            return taskGenerateMessage;
        }
    }
}
