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
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.workflow.*;
import com.wanghao.eldercare.eldercaresystem.dto.careteam.CareTeamAssignmentDTO;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.*;
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import com.wanghao.eldercare.eldercaresystem.entity.admission.Bed;
import com.wanghao.eldercare.eldercaresystem.entity.profile.ElderProfileEntity;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.AdmissionRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.BedRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.ElderProfileRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.careteam.CareTeamAssignmentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdmissionWorkflowOrchestrator {

    private static final long HEALTH_ASSESS_DUE_HOURS = 24L;
    private static final long BED_RESERVE_DUE_HOURS = 4L;
    private static final long CONTRACT_CONFIRM_DUE_HOURS = 24L;
    private static final long CHECK_IN_CONFIRM_DUE_HOURS = 4L;

    private final WfTaskRepository wfTaskRepository;
    private final WfTaskActionRepository wfTaskActionRepository;
    private final WfInstanceRepository wfInstanceRepository;
    private final AdmissionRecordRepository admissionRecordRepository;
    private final BedRepository bedRepository;
    private final CareTeamAssignmentRepository careTeamAssignmentRepository;
    private final CareTeamAssignmentService careTeamAssignmentService;
    private final ElderProfileRepository elderProfileRepository;
    private final ObjectMapper objectMapper;

    public AdmissionWorkflowOrchestrator(WfTaskRepository wfTaskRepository,
                                         WfTaskActionRepository wfTaskActionRepository,
                                         WfInstanceRepository wfInstanceRepository,
                                         AdmissionRecordRepository admissionRecordRepository,
                                         BedRepository bedRepository,
                                         CareTeamAssignmentRepository careTeamAssignmentRepository,
                                         CareTeamAssignmentService careTeamAssignmentService,
                                         ElderProfileRepository elderProfileRepository,
                                         ObjectMapper objectMapper) {
        this.wfTaskRepository = wfTaskRepository;
        this.wfTaskActionRepository = wfTaskActionRepository;
        this.wfInstanceRepository = wfInstanceRepository;
        this.admissionRecordRepository = admissionRecordRepository;
        this.bedRepository = bedRepository;
        this.careTeamAssignmentRepository = careTeamAssignmentRepository;
        this.careTeamAssignmentService = careTeamAssignmentService;
        this.elderProfileRepository = elderProfileRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WfTaskDTO complete(CurrentUser currentUser,
                              Long wfTaskId,
                              CompleteWfTaskRequest request,
                              WfTask task,
                              WfInstance instance) {
        ensureCanComplete(currentUser, task, instance);
        ensureTaskStatus(task);
        ensureAdmissionInstance(instance);

        LocalDateTime now = LocalDateTime.now();
        JsonNode formData = resolveFormData(request);
        String formDataJson = toJson(formData);
        String attachmentsJson = toJson(request.getAttachments());
        int updated = wfTaskRepository.completeIfPendingOrClaimed(
                wfTaskId,
                now,
                request.getComment(),
                formDataJson,
                attachmentsJson
        );
        if (updated == 0) {
            throw badRequest("任务状态不匹配，当前状态=" + loadTask(wfTaskId).getStatus());
        }

        WfTask completedTask = loadTask(wfTaskId);
        AdmissionRecord admission = admissionRecordRepository.findById(instance.getBizId())
                .orElseThrow(() -> new NotFoundException("入住记录不存在"));

        switch (completedTask.getNodeKey()) {
            case "assign_nurse" -> onAssignNurse(admission, instance, formData, currentUser.getUserId(), now);
            case "health_assess" -> onHealthAssess(admission, instance, formData, currentUser.getUserId(), now);
            case "bed_reserve" -> onBedReserve(admission, instance, formData, currentUser.getUserId(), now);
            case "contract_deposit_confirm" -> onContractDepositConfirm(admission, instance, formData, currentUser.getUserId(), now);
            case "check_in_confirm" -> onCheckInConfirm(admission, instance, now);
            default -> throw badRequest("不支持的 admission 节点: " + completedTask.getNodeKey());
        }

        saveAction(
                completedTask.getWfTaskId(),
                "complete",
                currentUser.getUserId(),
                request.getComment(),
                summarizeFormData(completedTask.getNodeKey(), formData),
                null,
                now
        );

        return enrichTask(completedTask);
    }

    private WfTask onAssignNurse(AdmissionRecord admission,
                                 WfInstance instance,
                                 JsonNode formData,
                                 Long actorId,
                                 LocalDateTime now) {
        List<Long> nurseIds = resolveNurseIds(formData);
        List<CareTeamAssignmentDTO> assignments = careTeamAssignmentService.upsertNursesByElderId(admission.getElderId(), nurseIds);
        List<Long> familyIds = resolveOptionalIds(formData, "familyIds", "familyId");
        int familyCount = 0;
        if (!familyIds.isEmpty()) {
            familyCount = careTeamAssignmentService.upsertFamiliesByElderId(admission.getElderId(), familyIds).size();
        }
        return createTask(
                instance.getInstanceId(),
                "health_assess",
                "健康评估",
                null,
                "doctor",
                now.plusHours(HEALTH_ASSESS_DUE_HOURS),
                actorId,
                "已绑定护理员数量=" + assignments.size() + "，家属数量=" + familyCount,
                now
        );
    }

    private WfTask onHealthAssess(AdmissionRecord admission,
                                  WfInstance instance,
                                  JsonNode formData,
                                  Long actorId,
                                  LocalDateTime now) {
        ElderProfileEntity profile = elderProfileRepository.findById(admission.getElderId()).orElseGet(() -> {
            ElderProfileEntity created = new ElderProfileEntity();
            created.setElderId(admission.getElderId());
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            return created;
        });
        applyHealthAssessment(profile, formData);
        profile.setUpdatedAt(now);
        elderProfileRepository.save(profile);

        findRequiredNurseId(admission.getElderId());
        return createTask(
                instance.getInstanceId(),
                "bed_reserve",
                "床位确认/预占",
                null,
                null,
                now.plusHours(BED_RESERVE_DUE_HOURS),
                actorId,
                null,
                now
        );
    }

    private WfTask onBedReserve(AdmissionRecord admission,
                                WfInstance instance,
                                JsonNode formData,
                                Long actorId,
                                LocalDateTime now) {
        Long targetBedId = admission.getBedId();
        Long confirmBedId = readLong(formData, "confirmBedId");
        if (confirmBedId != null && !confirmBedId.equals(targetBedId)) {
            rollbackOldReservedBedIfNeeded(targetBedId);
            admission.setBedId(confirmBedId);
            admission.setUpdatedAt(now);
            admissionRecordRepository.save(admission);
            targetBedId = confirmBedId;
        }

        Bed currentBed = bedRepository.findById(targetBedId).orElseThrow(() -> new NotFoundException("床位不存在"));
        if ("available".equalsIgnoreCase(currentBed.getStatus())) {
            int reserved = bedRepository.reserveIfAvailable(targetBedId);
            if (reserved == 0) {
                Bed latestBed = bedRepository.findById(targetBedId).orElseThrow(() -> new NotFoundException("床位不存在"));
                throw badRequest("床位不可预占，当前状态=" + latestBed.getStatus());
            }
        } else if (!"reserved".equalsIgnoreCase(currentBed.getStatus())) {
            throw badRequest("床位不可预占，当前状态=" + currentBed.getStatus());
        }

        return createTask(
                instance.getInstanceId(),
                "contract_deposit_confirm",
                "押金/合同确认",
                null,
                "nurse_leader",
                now.plusHours(CONTRACT_CONFIRM_DUE_HOURS),
                actorId,
                null,
                now
        );
    }

    private WfTask onContractDepositConfirm(AdmissionRecord admission,
                                            WfInstance instance,
                                            JsonNode formData,
                                            Long actorId,
                                            LocalDateTime now) {
        BigDecimal depositAmount = requireDecimal(formData, "depositAmount");
        String contractNo = readText(formData, "contractNo");
        String packageName = readText(formData, "packageName");

        int updated = admissionRecordRepository.updateContractAndDeposit(
                admission.getAdmissionId(),
                depositAmount,
                trimToNull(contractNo),
                trimToNull(packageName),
                now
        );
        if (updated == 0) {
            throw badRequest("入住记录更新失败");
        }

        Long nurseId = findRequiredNurseId(admission.getElderId());
        return createTask(
                instance.getInstanceId(),
                "check_in_confirm",
                "入住确认",
                nurseId,
                null,
                now.plusHours(CHECK_IN_CONFIRM_DUE_HOURS),
                actorId,
                null,
                now
        );
    }

    private void onCheckInConfirm(AdmissionRecord admission, WfInstance instance, LocalDateTime now) {
        int admissionUpdated = admissionRecordRepository.activateIfPending(admission.getAdmissionId(), now);
        if (admissionUpdated == 0) {
            throw badRequest("入住状态不匹配，仅允许 pending -> active");
        }

        int bedUpdated = bedRepository.occupyIfReserved(admission.getBedId());
        if (bedUpdated == 0) {
            Bed bed = bedRepository.findById(admission.getBedId()).orElseThrow(() -> new NotFoundException("床位不存在"));
            throw badRequest("床位状态不匹配，当前状态=" + bed.getStatus());
        }

        int instanceUpdated = wfInstanceRepository.completeIfRunning(instance.getInstanceId(), now);
        if (instanceUpdated == 0) {
            throw badRequest("流程实例状态不匹配，当前状态=" + instance.getStatus());
        }
    }

    private void ensureAdmissionInstance(WfInstance instance) {
        if (!"admission".equalsIgnoreCase(instance.getProcessKey())
                || !"admission".equalsIgnoreCase(instance.getBizType())) {
            throw badRequest("当前任务不属于 admission 流程");
        }
        if (!"running".equalsIgnoreCase(instance.getStatus())) {
            throw badRequest("流程实例状态不匹配，当前状态=" + instance.getStatus());
        }
    }

    private void ensureTaskStatus(WfTask task) {
        String status = task.getStatus() == null ? "" : task.getStatus().toLowerCase(Locale.ROOT);
        if (!"pending".equals(status) && !"claimed".equals(status)) {
            throw badRequest("任务状态不匹配，当前状态=" + task.getStatus());
        }
    }

    private void ensureCanComplete(CurrentUser currentUser, WfTask task, WfInstance instance) {
        if (isAdmissionCareTeamBedReserveTask(currentUser, task, instance)) {
            return;
        }
        if (task.getAssigneeId() != null) {
            if (isHealthAssessTask(task)
                    && (currentUser.hasRole("admin")
                    || currentUser.hasRole("nurse_leader")
                    || currentUser.hasRole("doctor"))) {
                return;
            }
            if (!task.getAssigneeId().equals(currentUser.getUserId())) {
                throw new AccessDeniedException("仅任务办理人可完成该节点");
            }
            return;
        }

        if (!StringUtils.hasText(task.getCandidateRole())) {
            throw new AccessDeniedException("任务未配置办理角色");
        }
        if (currentUser.hasRole("admin")) {
            return;
        }
        if (isHealthAssessTask(task) && currentUser.hasRole("nurse_leader")) {
            return;
        }
        if (!currentUser.hasRole(task.getCandidateRole())) {
            throw new AccessDeniedException("当前角色无权限办理该任务");
        }
    }

    private boolean isHealthAssessTask(WfTask task) {
        return task != null && "health_assess".equalsIgnoreCase(task.getNodeKey());
    }

    private boolean isAdmissionCareTeamBedReserveTask(CurrentUser currentUser, WfTask task, WfInstance instance) {
        if (currentUser == null || task == null || instance == null) {
            return false;
        }
        if (!"admission".equalsIgnoreCase(instance.getBizType())
                || !"bed_reserve".equalsIgnoreCase(task.getNodeKey())) {
            return false;
        }
        if (!(currentUser.hasRole("nurse") || currentUser.hasRole("caregiver"))) {
            return false;
        }
        AdmissionRecord admission = admissionRecordRepository.findById(instance.getBizId()).orElse(null);
        return admission != null && careTeamAssignmentRepository.existsActiveByElderIdAndNurseId(
                admission.getElderId(),
                currentUser.getUserId()
        );
    }

    private void applyHealthAssessment(ElderProfileEntity profile, JsonNode formData) {
        if (formData == null || !formData.isObject()) {
            return;
        }
        String gender = readText(formData, "gender");
        if (gender != null) {
            profile.setGender(normalizeGender(gender));
        }

        LocalDate birthday = readDate(formData, "birthday", "birthDate", "birth_date");
        if (birthday != null) {
            profile.setBirthday(birthday);
        }

        String address = firstNonBlank(
                readText(formData, "address"),
                readText(formData, "homeAddress"),
                readText(formData, "home_address"));
        if (address != null) {
            profile.setAddress(address);
        }

        String emergencyContactName = firstNonBlank(
                readText(formData, "emergencyContactName"),
                readText(formData, "emergencyName"),
                readText(formData, "emergency_name"),
                readText(formData, "emergency_contact_name"));
        if (emergencyContactName != null) {
            profile.setEmergencyContactName(emergencyContactName);
        }

        String emergencyContactPhone = firstNonBlank(
                readText(formData, "emergencyContactPhone"),
                readText(formData, "emergencyPhone"),
                readText(formData, "emergency_phone"),
                readText(formData, "emergency_contact_phone"));
        if (emergencyContactPhone != null) {
            profile.setEmergencyContactPhone(emergencyContactPhone);
        }

        String allergies = firstNonBlank(
                readText(formData, "allergies"),
                readText(formData, "allergy"),
                readText(formData, "allergyHistory"),
                readText(formData, "allergy_history"));
        if (allergies != null) {
            profile.setAllergies(allergies);
        }

        String chronicConditions = firstNonBlank(
                readText(formData, "chronicConditions"),
                readText(formData, "chronic"),
                readText(formData, "chronic_conditions"),
                readText(formData, "medicalHistory"),
                readText(formData, "medical_history"));
        if (chronicConditions != null) {
            profile.setChronicConditions(chronicConditions);
        }

        String dietTaboo = firstNonBlank(
                readText(formData, "dietTaboo"),
                readText(formData, "dietTaboos"),
                readText(formData, "diet_taboos"),
                readText(formData, "diet_taboo"));
        if (dietTaboo != null) {
            profile.setDietTaboo(dietTaboo);
        }

        String careLevel = firstNonBlank(
                readText(formData, "careLevel"),
                readText(formData, "careLevelSuggestion"),
                readText(formData, "care_level"),
                readText(formData, "level"));
        if (careLevel != null) {
            profile.setCareLevel(careLevel);
        }

        String notes = trimToNull(readText(formData, "notes"));
        if (notes != null) {
            profile.setNotes(notes);
        }
    }

    private Long findRequiredNurseId(Long elderId) {
        List<Long> nurseIds = careTeamAssignmentRepository.findActiveNurseIdsByElderId(elderId);
        if (nurseIds == null || nurseIds.isEmpty()) {
            throw badRequest("未绑定护理员，无法继续流程");
        }
        return nurseIds.get(0);
    }

    private void rollbackOldReservedBedIfNeeded(Long oldBedId) {
        Bed oldBed = bedRepository.findById(oldBedId).orElseThrow(() -> new NotFoundException("原床位不存在"));
        if ("reserved".equalsIgnoreCase(oldBed.getStatus())) {
            bedRepository.releaseAsAvailableIfReserved(oldBedId);
        }
    }

    private WfTask createTask(Long instanceId,
                              String nodeKey,
                              String taskName,
                              Long assigneeId,
                              String candidateRole,
                              LocalDateTime dueAt,
                              Long actorId,
                              String comment,
                              LocalDateTime now) {
        WfTask task = new WfTask();
        task.setInstanceId(instanceId);
        task.setNodeKey(nodeKey);
        task.setTaskName(taskName);
        task.setAssigneeId(assigneeId);
        task.setCandidateRole(candidateRole);
        task.setStatus("pending");
        task.setDueAt(dueAt);
        task.setCreatedAt(now);
        WfTask savedTask = wfTaskRepository.save(task);
        saveAction(savedTask.getWfTaskId(), "create", actorId, comment, null, null, now);
        return savedTask;
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

    private Long requireLong(JsonNode formData, String field) {
        Long value = readLong(formData, field);
        if (value == null) {
            throw badRequest(field + " 为必填");
        }
        return value;
    }

    private Long readLong(JsonNode formData, String field) {
        if (formData == null || !formData.has(field) || formData.get(field).isNull()) {
            return null;
        }
        JsonNode node = formData.get(field);
        if (node.isNumber()) {
            return node.longValue();
        }
        if (node.isTextual() && node.asText().matches("\\d+")) {
            return Long.parseLong(node.asText());
        }
        throw badRequest(field + " 格式非法");
    }

    private List<Long> resolveNurseIds(JsonNode formData) {
        List<Long> nurseIds = readLongList(formData, "nurseIds");
        if (!nurseIds.isEmpty()) {
            return nurseIds;
        }
        Long singleNurseId = readLong(formData, "nurseId");
        if (singleNurseId == null) {
            throw badRequest("nurseIds 或 nurseId 为必填");
        }
        return List.of(singleNurseId);
    }

    private List<Long> readLongList(JsonNode formData, String field) {
        if (formData == null || !formData.has(field) || formData.get(field).isNull()) {
            return List.of();
        }
        JsonNode node = formData.get(field);
        if (!node.isArray()) {
            throw badRequest(field + " 格式非法");
        }
        LinkedHashSet<Long> deduplicated = new LinkedHashSet<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull()) {
                continue;
            }
            if (item.isNumber()) {
                deduplicated.add(item.longValue());
                continue;
            }
            if (item.isTextual() && item.asText().matches("\\d+")) {
                deduplicated.add(Long.parseLong(item.asText()));
                continue;
            }
            throw badRequest(field + " 内含非法值");
        }
        return new ArrayList<>(deduplicated);
    }

    private List<Long> resolveOptionalIds(JsonNode formData, String arrayField, String singleField) {
        List<Long> values = readLongList(formData, arrayField);
        if (!values.isEmpty()) {
            return values;
        }
        Long singleValue = readLong(formData, singleField);
        if (singleValue == null) {
            return List.of();
        }
        return List.of(singleValue);
    }

    private BigDecimal requireDecimal(JsonNode formData, String field) {
        if (formData == null || !formData.has(field) || formData.get(field).isNull()) {
            throw badRequest(field + " 为必填");
        }
        JsonNode node = formData.get(field);
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual()) {
            try {
                return new BigDecimal(node.asText().trim());
            } catch (NumberFormatException ex) {
                throw badRequest(field + " 格式非法");
            }
        }
        throw badRequest(field + " 格式非法");
    }

    private String readText(JsonNode formData, String field) {
        if (formData == null || !formData.has(field) || formData.get(field).isNull()) {
            return null;
        }
        return formData.get(field).asText(null);
    }

    private LocalDate readDate(JsonNode formData, String... fields) {
        for (String field : fields) {
            String value = readText(formData, field);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                return LocalDate.parse(value.trim());
            } catch (Exception ex) {
                throw badRequest(field + " 格式非法，需为 yyyy-MM-dd");
            }
        }
        return null;
    }

    private String normalizeGender(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!List.of("male", "female", "unknown").contains(normalized)) {
            throw badRequest("gender 仅支持 male/female/unknown");
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String summarizeFormData(String nodeKey, JsonNode formData) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("nodeKey", nodeKey);
        if (formData != null && formData.isObject()) {
            summary.put("fields", formData.fieldNames().hasNext() ? iterable(formData.fieldNames()) : List.of());
        } else {
            summary.put("fields", List.of());
        }
        return toJson(summary);
    }

    private List<String> iterable(java.util.Iterator<String> iterator) {
        java.util.ArrayList<String> fields = new java.util.ArrayList<>();
        iterator.forEachRemaining(fields::add);
        return fields;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw badRequest("JSON 序列化失败");
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
