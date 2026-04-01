package com.wanghao.eldercare.eldercaresystem.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.workflow.*;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.Alarm;
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.AdmissionRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {

    private static final Set<String> DEFAULT_TODO_STATUS = Set.of("pending", "claimed");

    private final WfInstanceRepository wfInstanceRepository;
    private final WfTaskRepository wfTaskRepository;
    private final WfTaskActionRepository wfTaskActionRepository;
    private final AlarmRepository alarmRepository;
    private final AdmissionRecordRepository admissionRecordRepository;
    private final CareTeamAssignmentRepository careTeamAssignmentRepository;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final AdmissionWorkflowOrchestrator admissionWorkflowOrchestrator;

    public WorkflowService(WfInstanceRepository wfInstanceRepository,
                           WfTaskRepository wfTaskRepository,
                           WfTaskActionRepository wfTaskActionRepository,
                           AlarmRepository alarmRepository,
                           AdmissionRecordRepository admissionRecordRepository,
                           CareTeamAssignmentRepository careTeamAssignmentRepository,
                           PermissionService permissionService,
                           ObjectMapper objectMapper,
                           AdmissionWorkflowOrchestrator admissionWorkflowOrchestrator) {
        this.wfInstanceRepository = wfInstanceRepository;
        this.wfTaskRepository = wfTaskRepository;
        this.wfTaskActionRepository = wfTaskActionRepository;
        this.alarmRepository = alarmRepository;
        this.admissionRecordRepository = admissionRecordRepository;
        this.careTeamAssignmentRepository = careTeamAssignmentRepository;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
        this.admissionWorkflowOrchestrator = admissionWorkflowOrchestrator;
    }

    @Transactional
    public CreateWfInstanceResponse createInstance(CurrentUser currentUser, CreateWfInstanceRequest request) {
        WfInstance instance = new WfInstance();
        instance.setProcessKey(request.getProcessKey());
        instance.setBizType(request.getBizType());
        instance.setBizId(request.getBizId());
        instance.setStatus("running");
        instance.setStartedBy(currentUser.getUserId());

        LocalDateTime now = LocalDateTime.now();
        instance.setStartedAt(now);
        instance.setCreatedAt(now);

        WfInstance saved = wfInstanceRepository.save(instance);
        return new CreateWfInstanceResponse(saved.getInstanceId());
    }

    @Transactional(readOnly = true)
    public WfInstanceDetailDTO getInstanceByBiz(CurrentUser currentUser, String bizType, Long bizId) {
        WfInstance instance = wfInstanceRepository.findByBizTypeAndBizId(bizType, bizId)
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));

        ensureCanAccessInstance(currentUser, instance);
        return toDetail(instance);
    }

    @Transactional(readOnly = true)
    public WfTaskListResponse listMyTasks(CurrentUser currentUser,
                                          Collection<String> statuses,
                                          int page,
                                          int size) {
        Set<String> queryStatuses = normalizeStatuses(statuses);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<WfTask> taskPage;
        if (isAdminOrLeader(currentUser)) {
            taskPage = wfTaskRepository.findAllByStatusIn(queryStatuses, pageable);
        } else if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            List<WfTask> tasks = mergeNurseCareTeamTasks(currentUser, queryStatuses);
            int fromIndex = Math.min(Math.max(page, 0) * size, tasks.size());
            int toIndex = Math.min(fromIndex + size, tasks.size());
            WfTaskListResponse response = new WfTaskListResponse();
            response.setContent(tasks.subList(fromIndex, toIndex).stream().map(WfTaskDTO::from).toList());
            response.setTotalElements(tasks.size());
            response.setPage(page);
            response.setSize(size);
            return response;
        } else {
            taskPage = wfTaskRepository.findMyTodo(currentUser.getUserId(), currentUser.getRole(), queryStatuses, pageable);
        }

        WfTaskListResponse response = new WfTaskListResponse();
        response.setContent(taskPage.getContent().stream().map(WfTaskDTO::from).toList());
        response.setTotalElements(taskPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional
    public WfTaskDTO claim(CurrentUser currentUser, Long wfTaskId) {
        WfTask task = wfTaskRepository.findById(wfTaskId)
                .orElseThrow(() -> new NotFoundException("流程任务不存在"));

        ensureCanOperateTask(currentUser, task);

        if ("claimed".equals(task.getStatus()) && currentUser.getUserId().equals(task.getAssigneeId())) {
            return enrichTask(task);
        }
        if (!"pending".equals(task.getStatus())) {
            throw badRequest("任务状态不匹配，当前状态=" + task.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = wfTaskRepository.claimIfPending(wfTaskId, currentUser.getUserId(), now);
        if (updated == 0) {
            String currentStatus = wfTaskRepository.findById(wfTaskId).map(WfTask::getStatus).orElse("unknown");
            throw badRequest("任务已被领取或状态不匹配，当前状态=" + currentStatus);
        }

        saveAction(wfTaskId, "claim", currentUser.getUserId(), "领取任务", null, null, now);
        return enrichTask(loadTask(wfTaskId));
    }

    @Transactional
    public WfTaskDTO complete(CurrentUser currentUser, Long wfTaskId, CompleteWfTaskRequest request) {
        WfTask task = wfTaskRepository.findById(wfTaskId)
                .orElseThrow(() -> new NotFoundException("流程任务不存在"));
        WfInstance instance = wfInstanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));

        if ("admission".equalsIgnoreCase(instance.getProcessKey())) {
            return admissionWorkflowOrchestrator.complete(currentUser, wfTaskId, request, task, instance);
        }

        ensureCanOperateTask(currentUser, task);

        if ("completed".equals(task.getStatus())) {
            return enrichTask(task);
        }
        if (!"pending".equals(task.getStatus()) && !"claimed".equals(task.getStatus())) {
            throw badRequest("任务状态不匹配，当前状态=" + task.getStatus());
        }

        if ("claimed".equals(task.getStatus()) && task.getAssigneeId() != null
                && !currentUser.getUserId().equals(task.getAssigneeId()) && !isAdminOrLeader(currentUser)) {
            throw new AccessDeniedException("任务已被他人领取");
        }

        JsonNode effectiveFormData = resolveFormData(request);
        String formDataJson = toJson(effectiveFormData);
        String attachmentsJson = toJson(request.getAttachments());
        LocalDateTime now = LocalDateTime.now();

        int updated = wfTaskRepository.completeIfPendingOrClaimed(
                wfTaskId,
                now,
                request.getComment(),
                formDataJson,
                attachmentsJson
        );
        if (updated == 0) {
            String currentStatus = wfTaskRepository.findById(wfTaskId).map(WfTask::getStatus).orElse("unknown");
            throw badRequest("任务状态不匹配，当前状态=" + currentStatus);
        }

        String action = request.getAction() == null || request.getAction().isBlank()
                ? "complete"
                : request.getAction().toLowerCase(Locale.ROOT);
        saveAction(wfTaskId, action, currentUser.getUserId(), request.getComment(), formDataJson, attachmentsJson, now);
        return enrichTask(loadTask(wfTaskId));
    }

    @Transactional
    public Long startAlarmWorkflow(Long alarmId, Long startedBy) {
        WfInstance instance = new WfInstance();
        LocalDateTime now = LocalDateTime.now();

        instance.setProcessKey("alarm_process");
        instance.setBizType("alarm");
        instance.setBizId(alarmId);
        instance.setStatus("running");
        instance.setStartedBy(startedBy);
        instance.setStartedAt(now);
        instance.setCreatedAt(now);

        WfInstance savedInstance = wfInstanceRepository.save(instance);

        WfTask task = new WfTask();
        task.setInstanceId(savedInstance.getInstanceId());
        task.setNodeKey("accept_alarm");
        task.setTaskName("报警接单");
        task.setCandidateRole("nurse");
        task.setStatus("pending");
        task.setDueAt(now.plusMinutes(2));
        task.setCreatedAt(now);

        WfTask savedTask = wfTaskRepository.save(task);
        saveAction(savedTask.getWfTaskId(), "create", startedBy, "报警创建，生成接单任务", null, null, now);

        return savedInstance.getInstanceId();
    }

    @Transactional
    public void appendAlarmAction(Long alarmId, Long actorId, String action, String comment) {
        WfInstance instance = wfInstanceRepository.findByBizTypeAndBizId("alarm", alarmId).orElse(null);
        if (instance == null) {
            return;
        }

        WfTask latestTask = wfTaskRepository.findFirstByInstanceIdOrderByCreatedAtDesc(instance.getInstanceId()).orElse(null);
        if (latestTask != null) {
            saveAction(latestTask.getWfTaskId(), action, actorId, comment, null, null, LocalDateTime.now());
        }

        if ("accept".equals(action)) {
            createAlarmNodeTask(instance.getInstanceId(), "arrive_alarm", "报警到场", "nurse", LocalDateTime.now().plusMinutes(5));
        } else if ("arrive".equals(action)) {
            createAlarmNodeTask(instance.getInstanceId(), "close_alarm", "报警结案", "nurse", LocalDateTime.now().plusMinutes(10));
        } else if ("close".equals(action)) {
            instance.setStatus("completed");
            instance.setEndedAt(LocalDateTime.now());
            wfInstanceRepository.save(instance);
        }
    }

    private void createAlarmNodeTask(Long instanceId,
                                     String nodeKey,
                                     String taskName,
                                     String candidateRole,
                                     LocalDateTime dueAt) {
        WfTask task = new WfTask();
        LocalDateTime now = LocalDateTime.now();
        task.setInstanceId(instanceId);
        task.setNodeKey(nodeKey);
        task.setTaskName(taskName);
        task.setCandidateRole(candidateRole);
        task.setStatus("pending");
        task.setDueAt(dueAt);
        task.setCreatedAt(now);
        wfTaskRepository.save(task);
    }

    private WfTask loadTask(Long wfTaskId) {
        return wfTaskRepository.findById(wfTaskId)
                .orElseThrow(() -> new NotFoundException("流程任务不存在"));
    }

    private WfTaskDTO enrichTask(WfTask task) {
        WfTaskDTO dto = WfTaskDTO.from(task);
        dto.setActions(wfTaskActionRepository.findByWfTaskIdOrderByActionTimeAsc(task.getWfTaskId())
                .stream()
                .map(WfTaskActionDTO::from)
                .toList());
        return dto;
    }

    private WfInstanceDetailDTO toDetail(WfInstance instance) {
        WfInstanceDetailDTO dto = new WfInstanceDetailDTO();
        dto.setInstanceId(instance.getInstanceId());
        dto.setProcessKey(instance.getProcessKey());
        dto.setBizType(instance.getBizType());
        dto.setBizId(instance.getBizId());
        dto.setStatus(instance.getStatus());
        dto.setStartedBy(instance.getStartedBy());
        dto.setStartedAt(instance.getStartedAt());

        List<WfTaskDTO> taskDTOList = wfTaskRepository.findByInstanceIdOrderByCreatedAtAsc(instance.getInstanceId())
                .stream()
                .map(this::enrichTask)
                .toList();
        dto.setTasks(taskDTOList);

        return dto;
    }

    private void ensureCanAccessInstance(CurrentUser currentUser, WfInstance instance) {
        if (isAdminOrLeader(currentUser)) {
            return;
        }

        if (currentUser.hasRole("doctor") && "admission".equalsIgnoreCase(instance.getBizType())) {
            return;
        }

        if (isAdmissionCareTeamBedReserveInstanceAccessible(currentUser, instance)) {
            return;
        }

        if (wfTaskRepository.existsByInstanceIdAndAssigneeId(instance.getInstanceId(), currentUser.getUserId())
                || wfTaskRepository.existsByInstanceIdAndCandidateRole(instance.getInstanceId(), currentUser.getRole())) {
            return;
        }

        if (instance.getStartedBy() != null && instance.getStartedBy().equals(currentUser.getUserId())) {
            return;
        }

        if ("alarm".equalsIgnoreCase(instance.getBizType())) {
            Alarm alarm = alarmRepository.findById(instance.getBizId()).orElse(null);
            if (alarm != null) {
                permissionService.assertCanAccessElder(currentUser, alarm.getElderId());
                return;
            }
        }

        throw new AccessDeniedException("无权限访问该流程实例");
    }

    private void ensureCanOperateTask(CurrentUser currentUser, WfTask task) {
        if (isAdminOrLeader(currentUser)) {
            return;
        }

        WfInstance instance = wfInstanceRepository.findById(task.getInstanceId()).orElse(null);

        if (isAdmissionHealthAssessTask(instance, task)
                && (currentUser.hasRole("doctor") || currentUser.hasRole("nurse_leader"))) {
            return;
        }

        if (isAdmissionCareTeamBedReserveTask(currentUser, instance, task)) {
            return;
        }

        if (task.getAssigneeId() != null && task.getAssigneeId().equals(currentUser.getUserId())) {
            return;
        }

        if ("pending".equals(task.getStatus()) && currentUser.getRole() != null
                && currentUser.getRole().equalsIgnoreCase(task.getCandidateRole())) {
            return;
        }

        if (instance != null && "alarm".equalsIgnoreCase(instance.getBizType())) {
            Alarm alarm = alarmRepository.findById(instance.getBizId()).orElse(null);
            if (alarm != null) {
                permissionService.assertCanAccessElder(currentUser, alarm.getElderId());
                return;
            }
        }

        throw new AccessDeniedException("无权限操作该流程任务");
    }

    private boolean isAdmissionHealthAssessTask(WfInstance instance, WfTask task) {
        return instance != null
                && "admission".equalsIgnoreCase(instance.getBizType())
                && task != null
                && "health_assess".equalsIgnoreCase(task.getNodeKey());
    }

    private boolean isAdmissionCareTeamBedReserveTask(CurrentUser currentUser, WfInstance instance, WfTask task) {
        return currentUser != null
                && instance != null
                && task != null
                && "admission".equalsIgnoreCase(instance.getBizType())
                && "bed_reserve".equalsIgnoreCase(task.getNodeKey())
                && isAdmissionCareTeamMember(currentUser, instance.getBizId());
    }

    private boolean isAdmissionCareTeamBedReserveInstanceAccessible(CurrentUser currentUser, WfInstance instance) {
        return currentUser != null
                && instance != null
                && "admission".equalsIgnoreCase(instance.getBizType())
                && isAdmissionCareTeamMember(currentUser, instance.getBizId());
    }

    private boolean isAdmissionCareTeamMember(CurrentUser currentUser, Long admissionId) {
        if (currentUser == null || admissionId == null) {
            return false;
        }
        if (!(currentUser.hasRole("nurse") || currentUser.hasRole("caregiver"))) {
            return false;
        }
        AdmissionRecord admission = admissionRecordRepository.findById(admissionId).orElse(null);
        return admission != null && careTeamAssignmentRepository.existsActiveByElderIdAndNurseId(
                admission.getElderId(),
                currentUser.getUserId()
        );
    }

    private List<WfTask> mergeNurseCareTeamTasks(CurrentUser currentUser, Set<String> queryStatuses) {
        List<WfTask> directTasks = wfTaskRepository.findMyTodo(
                currentUser.getUserId(),
                currentUser.getRole(),
                queryStatuses,
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        List<WfTask> teamTasks = wfTaskRepository.findAdmissionBedReserveTodoForCareTeam(currentUser.getUserId(), queryStatuses);
        Map<Long, WfTask> merged = new LinkedHashMap<>();
        for (WfTask task : directTasks) {
            merged.put(task.getWfTaskId(), task);
        }
        for (WfTask task : teamTasks) {
            merged.put(task.getWfTaskId(), task);
        }
        List<WfTask> tasks = new ArrayList<>(merged.values());
        tasks.sort(Comparator.comparing(WfTask::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());
        return tasks;
    }

    private void saveAction(Long wfTaskId,
                            String action,
                            Long actorId,
                            String comment,
                            String formDataJson,
                            String attachmentsJson,
                            LocalDateTime actionTime) {
        WfTaskAction wfTaskAction = new WfTaskAction();
        wfTaskAction.setWfTaskId(wfTaskId);
        wfTaskAction.setAction(action);
        wfTaskAction.setActorId(actorId);
        wfTaskAction.setActionTime(actionTime);
        wfTaskAction.setComment(comment);
        wfTaskAction.setExtraJson(toActionExtraJson(formDataJson, attachmentsJson));
        wfTaskActionRepository.save(wfTaskAction);
    }

    private String toActionExtraJson(String formDataJson, String attachmentsJson) {
        if (formDataJson == null && attachmentsJson == null) {
            return null;
        }
        Map<String, Object> extra = new LinkedHashMap<>();
        if (formDataJson != null) {
            extra.put("formDataJson", formDataJson);
        }
        if (attachmentsJson != null) {
            extra.put("attachmentsJson", attachmentsJson);
        }
        return toJson(extra);
    }

    private Set<String> normalizeStatuses(Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return DEFAULT_TODO_STATUS;
        }
        return statuses.stream()
                .map(s -> s == null ? "" : s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean isAdminOrLeader(CurrentUser currentUser) {
        return currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader");
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "JSON 序列化失败", HttpStatus.BAD_REQUEST);
        }
    }

    private JsonNode resolveFormData(CompleteWfTaskRequest request) {
        if (request.getFormData() != null) {
            return request.getFormData();
        }
        JsonNode topLevel = buildTopLevelFormData(request);
        if (topLevel != null) {
            return topLevel;
        }
        if (request.getFormDataJson() == null || request.getFormDataJson().isBlank()) {
            return null;
        }
        try {
            JsonNode parsed = objectMapper.readTree(request.getFormDataJson());
            if (parsed != null && parsed.isTextual()) {
                String inner = parsed.asText();
                if (inner != null && !inner.isBlank()) {
                    return objectMapper.readTree(inner);
                }
            }
            return parsed;
        } catch (JsonProcessingException e) {
            throw badRequest("formDataJson 格式非法");
        }
    }

    private JsonNode buildTopLevelFormData(CompleteWfTaskRequest request) {
        ObjectNode node = objectMapper.createObjectNode();
        boolean hasAny = false;
        if (request.getElderId() != null) {
            node.put("elderId", request.getElderId());
            hasAny = true;
        }
        if (request.getNurseId() != null) {
            node.put("nurseId", request.getNurseId());
            hasAny = true;
        }
        if (request.getNurseIds() != null && !request.getNurseIds().isEmpty()) {
            ArrayNode nurseIds = node.putArray("nurseIds");
            request.getNurseIds().stream().filter(java.util.Objects::nonNull).forEach(nurseIds::add);
            hasAny = true;
        }
        if (request.getFamilyId() != null) {
            node.put("familyId", request.getFamilyId());
            hasAny = true;
        }
        if (request.getFamilyIds() != null && !request.getFamilyIds().isEmpty()) {
            ArrayNode familyIds = node.putArray("familyIds");
            request.getFamilyIds().stream().filter(java.util.Objects::nonNull).forEach(familyIds::add);
            hasAny = true;
        }
        return hasAny ? node : null;
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }
}
