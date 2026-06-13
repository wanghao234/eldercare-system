package com.wanghao.eldercare.eldercaresystem.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
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
import com.wanghao.eldercare.eldercaresystem.entity.admission.Bed;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.AdmissionRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.BedRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.file.FileStorageService;
import java.net.URI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;

@Service
public class WorkflowService {

    private static final Set<String> DEFAULT_TODO_STATUS = Set.of("pending", "claimed");

    private final WfInstanceRepository wfInstanceRepository;
    private final WfTaskRepository wfTaskRepository;
    private final WfTaskActionRepository wfTaskActionRepository;
    private final WfDefinitionRepository wfDefinitionRepository;
    private final UserRepository userRepository;
    private final AlarmRepository alarmRepository;
    private final AdmissionRecordRepository admissionRecordRepository;
    private final BedRepository bedRepository;
    private final CareTeamAssignmentRepository careTeamAssignmentRepository;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final AdmissionWorkflowOrchestrator admissionWorkflowOrchestrator;
    private final AdmissionContractImportService admissionContractImportService;
    private final FileStorageService fileStorageService;
    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowTaskSyncService workflowTaskSyncService;
    private final WorkflowActionLogService workflowActionLogService;
    private final WorkflowUserGuard workflowUserGuard;
    private final TaskService flowableTaskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;

    public WorkflowService(WfInstanceRepository wfInstanceRepository,
                           WfTaskRepository wfTaskRepository,
                           WfTaskActionRepository wfTaskActionRepository,
                           WfDefinitionRepository wfDefinitionRepository,
                           UserRepository userRepository,
                           AlarmRepository alarmRepository,
                           AdmissionRecordRepository admissionRecordRepository,
                           BedRepository bedRepository,
                           CareTeamAssignmentRepository careTeamAssignmentRepository,
                           PermissionService permissionService,
                           ObjectMapper objectMapper,
                           AdmissionWorkflowOrchestrator admissionWorkflowOrchestrator,
                           AdmissionContractImportService admissionContractImportService,
                           FileStorageService fileStorageService,
                           WorkflowInstanceService workflowInstanceService,
                           WorkflowTaskSyncService workflowTaskSyncService,
                           WorkflowActionLogService workflowActionLogService,
                           WorkflowUserGuard workflowUserGuard,
                           TaskService flowableTaskService,
                           RuntimeService runtimeService,
                           HistoryService historyService,
                           RepositoryService repositoryService) {
        this.wfInstanceRepository = wfInstanceRepository;
        this.wfTaskRepository = wfTaskRepository;
        this.wfTaskActionRepository = wfTaskActionRepository;
        this.wfDefinitionRepository = wfDefinitionRepository;
        this.userRepository = userRepository;
        this.alarmRepository = alarmRepository;
        this.admissionRecordRepository = admissionRecordRepository;
        this.bedRepository = bedRepository;
        this.careTeamAssignmentRepository = careTeamAssignmentRepository;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
        this.admissionWorkflowOrchestrator = admissionWorkflowOrchestrator;
        this.admissionContractImportService = admissionContractImportService;
        this.fileStorageService = fileStorageService;
        this.workflowInstanceService = workflowInstanceService;
        this.workflowTaskSyncService = workflowTaskSyncService;
        this.workflowActionLogService = workflowActionLogService;
        this.workflowUserGuard = workflowUserGuard;
        this.flowableTaskService = flowableTaskService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.repositoryService = repositoryService;
    }

    @Transactional
    public CreateWfInstanceResponse createInstance(CurrentUser currentUser, CreateWfInstanceRequest request) {
        workflowUserGuard.requireActive(currentUser);
        return workflowInstanceService.start(currentUser, request);
    }

    @Transactional(readOnly = true)
    public WfInstanceDetailDTO getInstanceByBiz(CurrentUser currentUser, String bizType, Long bizId) {
        WfInstance instance = wfInstanceRepository.findByBizTypeAndBizId(bizType, bizId)
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));

        ensureCanAccessInstance(currentUser, instance);
        return toDetail(instance);
    }

    @Transactional
    public WfInstanceDetailDTO getInstanceById(CurrentUser currentUser, Long instanceId) {
        WfInstance instance = wfInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));
        ensureCanAccessInstance(currentUser, instance);
        workflowTaskSyncService.syncOpenTasks(instance, currentUser.getUserId());
        return toDetail(instance);
    }

    @Transactional
    public List<WfTaskDTO> listInstanceTasks(CurrentUser currentUser, Long instanceId) {
        WfInstance instance = wfInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));
        ensureCanAccessInstance(currentUser, instance);
        workflowTaskSyncService.syncOpenTasks(instance, currentUser.getUserId());
        return wfTaskRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId)
                .stream()
                .map(this::enrichTask)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WfTaskActionDTO> listInstanceActions(CurrentUser currentUser, Long instanceId) {
        WfInstance instance = wfInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));
        ensureCanAccessInstance(currentUser, instance);
        return wfTaskActionRepository.findByInstanceIdOrderByActionTimeAsc(instanceId)
                .stream()
                .map(WfTaskActionDTO::from)
                .toList();
    }

    @Transactional
    public WfInstanceDiagramDTO getInstanceDiagram(CurrentUser currentUser, Long instanceId) {
        WfInstance instance = wfInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));
        ensureCanAccessInstance(currentUser, instance);
        workflowTaskSyncService.syncOpenTasks(instance, currentUser.getUserId());

        WfInstanceDiagramDTO dto = new WfInstanceDiagramDTO();
        dto.setInstanceId(instance.getInstanceId());
        dto.setProcessKey(instance.getProcessKey());
        dto.setBizType(instance.getBizType());
        dto.setBizId(instance.getBizId());
        dto.setStatus(instance.getStatus());
        dto.setBpmnXml(resolveBpmnXml(instance));
        dto.setActiveNodeKeys(resolveActiveNodeKeys(instance));
        dto.setCompletedNodeKeys(resolveCompletedNodeKeys(instance));
        dto.setTaskNodes(wfTaskRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId)
                .stream()
                .map(this::enrichTask)
                .toList());
        dto.setActionLogs(wfTaskActionRepository.findByInstanceIdOrderByActionTimeAsc(instanceId)
                .stream()
                .map(WfTaskActionDTO::from)
                .toList());
        return dto;
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
            response.setContent(tasks.subList(fromIndex, toIndex).stream().map(this::enrichTask).toList());
            response.setTotalElements(tasks.size());
            response.setPage(page);
            response.setSize(size);
            return response;
        } else {
            taskPage = wfTaskRepository.findMyTodo(currentUser.getUserId(), currentUser.getRole(), queryStatuses, pageable);
        }

        WfTaskListResponse response = new WfTaskListResponse();
        response.setContent(taskPage.getContent().stream().map(this::enrichTask).toList());
        response.setTotalElements(taskPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional
    public WfTaskDTO claim(CurrentUser currentUser, Long wfTaskId) {
        workflowUserGuard.requireActive(currentUser);
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
        if (StringUtils.hasText(task.getExternalTaskId())) {
            ensureCanClaimFlowableTask(currentUser, task);
            try {
                flowableTaskService.claim(task.getExternalTaskId(), String.valueOf(currentUser.getUserId()));
            } catch (Exception ex) {
                throw badRequest("Flowable 任务领取失败: " + ex.getMessage());
            }
            task.setStatus("claimed");
            task.setAssigneeId(currentUser.getUserId());
            task.setClaimedAt(now);
            wfTaskRepository.save(task);
            WfTask savedTask = loadTask(wfTaskId);
            workflowActionLogService.log(savedTask, "claim", currentUser.getUserId(), "领取任务", null, null, now);
            return enrichTask(savedTask);
        }

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
        workflowUserGuard.requireActive(currentUser);
        WfTask task = wfTaskRepository.findById(wfTaskId)
                .orElseThrow(() -> new NotFoundException("流程任务不存在"));
        WfInstance instance = wfInstanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));

        if ("admission".equalsIgnoreCase(instance.getProcessKey()) && !StringUtils.hasText(task.getExternalTaskId())) {
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

        if (StringUtils.hasText(task.getExternalTaskId())) {
            ensureCanCompleteFlowableProjection(currentUser, task);
            completeFlowableTask(currentUser, task, instance, request, effectiveFormData, formDataJson, attachmentsJson, now);
            WfTask savedTask = loadTask(wfTaskId);
            String action = resolveAction(request);
            workflowActionLogService.log(savedTask, action, currentUser.getUserId(), request.getComment(), formDataJson, attachmentsJson, now);
            workflowTaskSyncService.syncOpenTasks(instance, currentUser.getUserId());
            return enrichTask(loadTask(wfTaskId));
        }

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

    @Transactional(readOnly = true)
    public byte[] downloadContractTemplate(CurrentUser currentUser, Long wfTaskId, CompleteWfTaskRequest request) {
        WfTask task = wfTaskRepository.findById(wfTaskId)
                .orElseThrow(() -> new NotFoundException("流程任务不存在"));
        WfInstance instance = wfInstanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));
        if (!"admission".equalsIgnoreCase(instance.getProcessKey())) {
            throw badRequest("当前流程不支持下载合同模板");
        }
        return admissionWorkflowOrchestrator.downloadContractTemplate(currentUser, request, task, instance);
    }

    @Transactional(readOnly = true)
    public String buildContractTemplateFileName(Long wfTaskId) {
        WfTask task = wfTaskRepository.findById(wfTaskId)
                .orElseThrow(() -> new NotFoundException("流程任务不存在"));
        WfInstance instance = wfInstanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));
        if (!"admission".equalsIgnoreCase(instance.getProcessKey())) {
            throw badRequest("当前流程不支持下载合同模板");
        }
        return admissionWorkflowOrchestrator.buildContractTemplateFileName(instance);
    }

    @Transactional
    public ImportAdmissionContractResponse importAdmissionContract(CurrentUser currentUser, MultipartFile file) {
        return admissionContractImportService.importContract(file);
    }

    @Transactional
    public ImportAdmissionContractResponse importAdmissionContract(CurrentUser currentUser, MultipartFile file, Long admissionId) {
        if (admissionId == null) {
            return admissionContractImportService.importContract(file);
        }
        AdmissionRecord admission = admissionRecordRepository.findById(admissionId)
                .orElseThrow(() -> new NotFoundException("入住记录不存在"));
        return admissionContractImportService.importContract(file, admission);
    }

    @Transactional
    public ImportAdmissionContractResponse importAdmissionContractByTask(CurrentUser currentUser, Long wfTaskId, MultipartFile file) {
        WfTask task = wfTaskRepository.findById(wfTaskId)
                .orElseThrow(() -> new NotFoundException("流程任务不存在"));
        WfInstance instance = wfInstanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));
        if (!"admission".equalsIgnoreCase(instance.getProcessKey())) {
            throw badRequest("当前流程不支持导入合同");
        }
        return admissionWorkflowOrchestrator.importContract(currentUser, file, task, instance);
    }

    @Transactional(readOnly = true)
    public byte[] downloadImportedContractByTask(CurrentUser currentUser, Long wfTaskId) {
        WfTask task = wfTaskRepository.findById(wfTaskId)
                .orElseThrow(() -> new NotFoundException("流程任务不存在"));
        WfInstance instance = wfInstanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new NotFoundException("流程实例不存在"));
        if (!"admission".equalsIgnoreCase(instance.getProcessKey())) {
            throw badRequest("当前流程不支持下载已上传合同");
        }
        return admissionWorkflowOrchestrator.downloadImportedContract(currentUser, task, instance);
    }

    @Transactional(readOnly = true)
    public byte[] downloadTaskAttachment(CurrentUser currentUser, Long wfTaskId, String attachmentUrl) {
        WfTask task = wfTaskRepository.findById(wfTaskId)
                .orElseThrow(() -> new NotFoundException("流程任务不存在"));
        ensureCanOperateTask(currentUser, task);
        String normalizedUrl = normalizeUploadUrl(attachmentUrl);
        ensureAttachmentBelongsToTask(task, normalizedUrl);
        return loadUploadFile(normalizedUrl);
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
        wfInstanceRepository.findById(task.getInstanceId()).ifPresent(instance -> {
            dto.setProcessKey(instance.getProcessKey());
            dto.setBizType(instance.getBizType());
            dto.setBizId(instance.getBizId());
        });
        if (task.getAssigneeId() != null) {
            userRepository.findByUserIdAndDeletedAtIsNull(task.getAssigneeId())
                    .map(User::getRealName)
                    .filter(StringUtils::hasText)
                    .ifPresent(dto::setAssigneeName);
        }
        dto.setActions(wfTaskActionRepository.findByWfTaskIdOrderByActionTimeAsc(task.getWfTaskId())
                .stream()
                .map(WfTaskActionDTO::from)
                .toList());
        return dto;
    }

    private void completeFlowableTask(CurrentUser currentUser,
                                      WfTask task,
                                      WfInstance instance,
                                      CompleteWfTaskRequest request,
                                      JsonNode effectiveFormData,
                                      String formDataJson,
                                      String attachmentsJson,
                                      LocalDateTime now) {
        Task flowableTask = flowableTaskService.createTaskQuery().taskId(task.getExternalTaskId()).singleResult();
        if (flowableTask == null) {
            throw badRequest("Flowable 任务不存在或已完成");
        }
        if (StringUtils.hasText(flowableTask.getAssignee())
                && !String.valueOf(currentUser.getUserId()).equals(flowableTask.getAssignee())) {
            throw new AccessDeniedException("任务已被他人领取");
        }
        Map<String, Object> variables = buildFlowableVariables(request, effectiveFormData);
        try {
            if (!StringUtils.hasText(flowableTask.getAssignee())) {
                flowableTaskService.claim(task.getExternalTaskId(), String.valueOf(currentUser.getUserId()));
                task.setAssigneeId(currentUser.getUserId());
                task.setClaimedAt(now);
            }
            applyAdmissionBusinessSideEffects(task, instance, formDataJson, now);
            flowableTaskService.complete(task.getExternalTaskId(), variables);
        } catch (Exception ex) {
            throw badRequest("Flowable 任务完成失败: " + ex.getMessage());
        }
        workflowTaskSyncService.markCompleted(task, request.getComment(), formDataJson, attachmentsJson, now);
    }

    private void applyAdmissionBusinessSideEffects(WfTask task, WfInstance instance, String formDataJson, LocalDateTime now) {
        if (!"admission".equalsIgnoreCase(instance.getProcessKey())) {
            return;
        }
        AdmissionRecord admission = admissionRecordRepository.findById(instance.getBizId()).orElse(null);
        if (admission == null) {
            return;
        }
        if ("bed_confirm".equalsIgnoreCase(task.getNodeKey())) {
            Long confirmBedId = readLongFromJson(formDataJson, "confirmBedId");
            if (confirmBedId != null && !confirmBedId.equals(admission.getBedId())) {
                admission.setBedId(confirmBedId);
                admission.setUpdatedAt(now);
                admissionRecordRepository.save(admission);
            }
            return;
        }
        if ("deposit_contract_confirm".equalsIgnoreCase(task.getNodeKey())) {
            applyDepositContractConfirm(admission, formDataJson, now);
            return;
        }
        if ("admission_confirm".equalsIgnoreCase(task.getNodeKey())) {
            int admissionUpdated = admissionRecordRepository.activateIfPending(admission.getAdmissionId(), now);
            if (admissionUpdated == 0) {
                throw badRequest("入住状态不匹配，仅允许 pending -> active");
            }
            int bedUpdated = bedRepository.occupyIfReserved(admission.getBedId());
            if (bedUpdated == 0) {
                Bed bed = bedRepository.findById(admission.getBedId()).orElseThrow(() -> new NotFoundException("床位不存在"));
                throw badRequest("床位状态不匹配，当前状态=" + bed.getStatus());
            }
        }
    }

    private void applyDepositContractConfirm(AdmissionRecord admission, String formDataJson, LocalDateTime now) {
        try {
            JsonNode node = StringUtils.hasText(formDataJson) ? objectMapper.readTree(formDataJson) : null;
            if (node == null || !node.isObject()) {
                return;
            }
            if (node.has("depositAmount") && !node.get("depositAmount").isNull()) {
                admission.setDepositAmount(node.get("depositAmount").decimalValue());
            }
            if (node.has("contractNo") && !node.get("contractNo").isNull()) {
                admission.setContractNo(node.get("contractNo").asText());
            }
            if (node.has("packageName") && !node.get("packageName").isNull()) {
                admission.setPackageName(node.get("packageName").asText());
            }
            admission.setUpdatedAt(now);
            admissionRecordRepository.save(admission);
        } catch (JsonProcessingException ex) {
            throw badRequest("formDataJson 格式非法");
        }
    }

    private Long readLongFromJson(String json, String field) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.has(field) || node.get(field).isNull()) {
                return null;
            }
            JsonNode value = node.get(field);
            if (value.isNumber()) {
                return value.longValue();
            }
            if (value.isTextual() && value.asText().matches("\\d+")) {
                return Long.parseLong(value.asText());
            }
            throw badRequest(field + " 格式非法");
        } catch (JsonProcessingException ex) {
            throw badRequest("formDataJson 格式非法");
        }
    }

    private Map<String, Object> buildFlowableVariables(CompleteWfTaskRequest request, JsonNode formData) {
        Map<String, Object> variables = new HashMap<>();
        if (formData != null && formData.isObject()) {
            variables.putAll(objectMapper.convertValue(formData, new TypeReference<Map<String, Object>>() {}));
        }
        if (request.getApproved() != null) {
            variables.put("approved", request.getApproved());
        }
        String action = request.getAction() == null ? "" : request.getAction().toLowerCase(Locale.ROOT);
        if ("approve".equals(action) || "approved".equals(action)) {
            variables.put("approved", true);
        } else if ("reject".equals(action) || "rejected".equals(action)) {
            variables.put("approved", false);
        }
        return variables;
    }

    private String resolveAction(CompleteWfTaskRequest request) {
        if (request.getAction() != null && !request.getAction().isBlank()) {
            return request.getAction().toLowerCase(Locale.ROOT);
        }
        if (Boolean.TRUE.equals(request.getApproved())) {
            return "approve";
        }
        if (Boolean.FALSE.equals(request.getApproved())) {
            return "reject";
        }
        JsonNode formData = request.getFormData();
        if (formData != null && formData.has("approved")) {
            return formData.path("approved").asBoolean() ? "approve" : "reject";
        }
        return "complete";
    }

    private void ensureCanClaimFlowableTask(CurrentUser currentUser, WfTask task) {
        if (task.getAssigneeId() != null) {
            if (!task.getAssigneeId().equals(currentUser.getUserId())) {
                throw new AccessDeniedException("任务已指派给其他办理人");
            }
            return;
        }
        if (!StringUtils.hasText(task.getCandidateRole())) {
            throw new AccessDeniedException("任务未配置候选角色");
        }
        if (!task.getCandidateRole().equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("当前角色无权限领取该任务");
        }
    }

    private void ensureCanCompleteFlowableProjection(CurrentUser currentUser, WfTask task) {
        if (task.getAssigneeId() != null) {
            if (!task.getAssigneeId().equals(currentUser.getUserId())) {
                throw new AccessDeniedException("仅任务办理人可完成该任务");
            }
            return;
        }
        if (!StringUtils.hasText(task.getCandidateRole())
                || !task.getCandidateRole().equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("当前角色无权限完成该任务");
        }
    }

    private String resolveBpmnXml(WfInstance instance) {
        if (StringUtils.hasText(instance.getExternalInstanceId())) {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(instance.getProcessKey())
                    .latestVersion()
                    .singleResult();
            String xml = readBpmnXml(definition);
            if (StringUtils.hasText(xml)) {
                return xml;
            }
        }
        return wfDefinitionRepository.findFirstByProcessKeyOrderByVersionDesc(instance.getProcessKey())
                .map(WfDefinition::getDefinitionJson)
                .orElse(null);
    }

    private String readBpmnXml(ProcessDefinition definition) {
        if (definition == null) {
            return null;
        }
        try (var stream = repositoryService.getResourceAsStream(
                definition.getDeploymentId(),
                definition.getResourceName())) {
            if (stream == null) {
                return null;
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    private List<String> resolveActiveNodeKeys(WfInstance instance) {
        if (!StringUtils.hasText(instance.getExternalInstanceId())
                || runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getExternalInstanceId())
                .singleResult() == null) {
            return List.of();
        }
        return runtimeService.getActiveActivityIds(instance.getExternalInstanceId());
    }

    private List<String> resolveCompletedNodeKeys(WfInstance instance) {
        if (!StringUtils.hasText(instance.getExternalInstanceId())) {
            return List.of();
        }
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instance.getExternalInstanceId())
                .activityType("userTask")
                .finished()
                .orderByHistoricActivityInstanceEndTime()
                .asc()
                .list()
                .stream()
                .map(HistoricActivityInstance::getActivityId)
                .distinct()
                .toList();
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
        dto.setEndedAt(instance.getEndedAt());
        dto.setEngineType(instance.getEngineType());
        dto.setExternalInstanceId(instance.getExternalInstanceId());

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

        if (isUnboundAlarmInstance(instance)) {
            throw new AccessDeniedException("未绑定老人的报警仅管理员或护士长可访问");
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
                assertCanAccessAlarm(currentUser, alarm);
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

        if (isUnboundAlarmInstance(instance)) {
            throw new AccessDeniedException("未绑定老人的报警仅管理员或护士长可操作");
        }

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
                assertCanAccessAlarm(currentUser, alarm);
                return;
            }
        }

        throw new AccessDeniedException("无权限操作该流程任务");
    }

    private boolean isAdmissionHealthAssessTask(WfInstance instance, WfTask task) {
        return instance != null
                && "admission".equalsIgnoreCase(instance.getBizType())
                && task != null
                && "health_assessment".equalsIgnoreCase(task.getNodeKey());
    }

    private boolean isAdmissionCareTeamBedReserveTask(CurrentUser currentUser, WfInstance instance, WfTask task) {
        return currentUser != null
                && instance != null
                && task != null
                && "admission".equalsIgnoreCase(instance.getBizType())
                && "bed_confirm".equalsIgnoreCase(task.getNodeKey())
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
        wfTaskRepository.findById(wfTaskId).map(WfTask::getInstanceId).ifPresent(wfTaskAction::setInstanceId);
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

    private boolean isUnboundAlarmInstance(WfInstance instance) {
        if (instance == null || !"alarm".equalsIgnoreCase(instance.getBizType())) {
            return false;
        }
        Alarm alarm = alarmRepository.findById(instance.getBizId()).orElse(null);
        return alarm != null && alarm.getElderId() == null;
    }

    private void assertCanAccessAlarm(CurrentUser currentUser, Alarm alarm) {
        if (alarm.getElderId() == null) {
            if (isAdminOrLeader(currentUser)) {
                return;
            }
            throw new AccessDeniedException("未绑定老人的报警仅管理员或护士长可访问");
        }
        permissionService.assertCanAccessElder(currentUser, alarm.getElderId());
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

    private void ensureAttachmentBelongsToTask(WfTask task, String attachmentUrl) {
        String attachmentsJson = task.getAttachmentsJson();
        if (!StringUtils.hasText(attachmentsJson)) {
            throw badRequest("该任务没有附件");
        }
        try {
            JsonNode node = objectMapper.readTree(attachmentsJson);
            if (!node.isArray()) {
                throw badRequest("任务附件格式非法");
            }
            boolean matched = false;
            for (JsonNode item : node) {
                String candidate = normalizeUploadUrl(item.asText(null));
                if (Objects.equals(candidate, attachmentUrl)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw badRequest("附件不属于该任务");
            }
        } catch (JsonProcessingException e) {
            throw badRequest("任务附件格式非法");
        }
    }

    private byte[] loadUploadFile(String uploadUrl) {
        String fileName = uploadUrl.substring(uploadUrl.lastIndexOf('/') + 1);
        Path basePath = fileStorageService.getStorageAbsolutePath();
        Path path = basePath.resolve(fileName).normalize();
        try {
            if (!path.startsWith(basePath) || !Files.exists(path)) {
                throw badRequest("附件文件不存在");
            }
            return Files.readAllBytes(path);
        } catch (Exception ex) {
            if (ex instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取附件失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String normalizeUploadUrl(String value) {
        if (!StringUtils.hasText(value)) {
            throw badRequest("附件地址不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                trimmed = URI.create(trimmed).getPath();
            } catch (Exception ex) {
                throw badRequest("附件地址格式非法");
            }
        }
        if (!StringUtils.hasText(trimmed) || !trimmed.startsWith("/uploads/")) {
            throw badRequest("仅支持下载 /uploads 下的附件");
        }
        return trimmed;
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
