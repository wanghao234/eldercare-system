package com.wanghao.eldercare.eldercaresystem.service.medication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.wanghao.eldercare.eldercaresystem.controller.medication.*;
import com.wanghao.eldercare.eldercaresystem.dto.medication.*;
import com.wanghao.eldercare.eldercaresystem.entity.medication.*;
import com.wanghao.eldercare.eldercaresystem.entity.task.Task;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.medication.*;
import com.wanghao.eldercare.eldercaresystem.mapper.task.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicationService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> PLAN_STATUS = Set.of("active", "paused", "ended");
    private static final Set<String> RECORD_STATUS = Set.of("given", "refused", "missed", "delayed", "stopped");

    private final MedicationRepository medicationRepository;
    private final MedicationPlanRepository medicationPlanRepository;
    private final MedicationAdminRecordRepository medicationAdminRecordRepository;
    private final PermissionService permissionService;
    private final TaskRepository taskRepository;
    private final CareTeamAssignmentRepository careTeamAssignmentRepository;
    private final ObjectMapper objectMapper;

    public MedicationService(MedicationRepository medicationRepository,
                             MedicationPlanRepository medicationPlanRepository,
                             MedicationAdminRecordRepository medicationAdminRecordRepository,
                             PermissionService permissionService,
                             TaskRepository taskRepository,
                             CareTeamAssignmentRepository careTeamAssignmentRepository,
                             ObjectMapper objectMapper) {
        this.medicationRepository = medicationRepository;
        this.medicationPlanRepository = medicationPlanRepository;
        this.medicationAdminRecordRepository = medicationAdminRecordRepository;
        this.permissionService = permissionService;
        this.taskRepository = taskRepository;
        this.careTeamAssignmentRepository = careTeamAssignmentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public MedicationListResponse listMedications(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String q = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Medication> result = medicationRepository.searchByKeyword(q, pageable);

        MedicationListResponse response = new MedicationListResponse();
        response.setContent(result.getContent().stream().map(MedicationDTO::from).toList());
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional
    public MedicationDTO createMedication(CurrentUser currentUser, CreateMedicationRequest request) {
        requireCatalogManager(currentUser);
        Medication medication = new Medication();
        medication.setMedicationName(request.getMedicationName().trim());
        medication.setSpec(request.getSpec());
        medication.setUnit(request.getUnit());
        medication.setDescription(request.getDescription());
        medication.setCreatedAt(LocalDateTime.now());
        return MedicationDTO.from(medicationRepository.save(medication));
    }

    @Transactional
    public MedicationDTO updateMedication(CurrentUser currentUser, Long id, CreateMedicationRequest request) {
        requireCatalogManager(currentUser);
        Medication medication = medicationRepository.findById(id).orElseThrow(() -> new NotFoundException("药品不存在"));
        medication.setMedicationName(request.getMedicationName().trim());
        medication.setSpec(request.getSpec());
        medication.setUnit(request.getUnit());
        medication.setDescription(request.getDescription());
        return MedicationDTO.from(medicationRepository.save(medication));
    }

    @Transactional
    public void deleteMedication(CurrentUser currentUser, Long id) {
        requireCatalogManager(currentUser);
        Medication medication = medicationRepository.findById(id).orElseThrow(() -> new NotFoundException("药品不存在"));
        if (medicationPlanRepository.findAll().stream().anyMatch(plan -> planReferencesMedication(plan, id))) {
            throw badRequest("药品已被用药计划引用，不能删除");
        }
        medicationRepository.delete(medication);
    }

    @Transactional(readOnly = true)
    public MedicationPlanListResponse listPlans(CurrentUser currentUser, Long elderId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedStatus = (status == null || status.isBlank()) ? null : normalizeStatus(status);

        Page<MedicationPlan> result;
        if (elderId != null) {
            assertCanReadElder(currentUser, elderId);
            if (normalizedStatus == null) {
                result = medicationPlanRepository.findByElderId(elderId, pageable);
            } else {
                result = medicationPlanRepository.findByElderIdAndStatus(elderId, normalizedStatus, pageable);
            }
        } else {
            List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
            if (visibleElderIds == null) {
                result = normalizedStatus == null
                        ? medicationPlanRepository.findAll(pageable)
                        : medicationPlanRepository.findByStatus(normalizedStatus, pageable);
            } else if (visibleElderIds.isEmpty()) {
                result = Page.empty(pageable);
            } else {
                result = normalizedStatus == null
                        ? medicationPlanRepository.findByElderIdIn(visibleElderIds, pageable)
                        : medicationPlanRepository.findByElderIdInAndStatus(visibleElderIds, normalizedStatus, pageable);
            }
        }

        MedicationPlanListResponse response = new MedicationPlanListResponse();
        response.setContent(result.getContent().stream().map(this::toPlanDTO).toList());
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional
    public MedicationPlanDTO createPlan(CurrentUser currentUser, CreateMedicationPlanRequest request) {
        requirePlanWriter(currentUser);
        permissionService.assertCanAccessElder(currentUser, request.getElderId());

        List<PlanMedicationItem> items = validateAndBuildPlanItems(request.getMedicationItems(),
                request.getMedicationId(),
                request.getDosage(),
                request.getFrequency(),
                request.getTimes());
        validateDateRange(request.getStartDate(), request.getEndDate());

        LocalDateTime now = LocalDateTime.now();
        MedicationPlan plan = new MedicationPlan();
        applyPlanItems(plan, items);
        plan.setElderId(request.getElderId());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setStatus("active");
        plan.setCreatedBy(currentUser.getUserId());
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);

        MedicationPlan saved = medicationPlanRepository.save(plan);
        createMedicationTasksForPlan(saved, currentUser.getUserId());
        return toPlanDTO(saved);
    }

    @Transactional
    public MedicationPlanDTO updatePlan(CurrentUser currentUser, Long planId, UpdateMedicationPlanRequest request) {
        requirePlanWriter(currentUser);
        MedicationPlan plan = medicationPlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("用药计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());

        if ("ended".equalsIgnoreCase(plan.getStatus())) {
            throw badRequest("已结束的用药计划不可编辑");
        }

        List<PlanMedicationItem> items = validateAndBuildPlanItems(request.getMedicationItems(),
                request.getMedicationId(),
                request.getDosage(),
                request.getFrequency(),
                request.getTimes());
        validateDateRange(request.getStartDate(), request.getEndDate());

        applyPlanItems(plan, items);
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setUpdatedAt(LocalDateTime.now());
        return toPlanDTO(medicationPlanRepository.save(plan));
    }

    @Transactional
    public MedicationPlanDTO patchPlanStatus(CurrentUser currentUser, Long planId, PatchMedicationPlanStatusRequest request) {
        requirePlanWriter(currentUser);
        MedicationPlan plan = medicationPlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("用药计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());

        String from = normalizeStatus(request.getFrom());
        String to = normalizeStatus(request.getTo());
        validatePlanTransition(from, to);

        int updated = medicationPlanRepository.updateStatusIfMatch(planId, from, to, LocalDateTime.now());
        if (updated == 0) {
            String currentStatus = medicationPlanRepository.findStatusByPlanId(planId).orElse("unknown");
            throw badRequest("状态不匹配，当前状态=" + currentStatus);
        }
        return toPlanDTO(medicationPlanRepository.findById(planId).orElseThrow(() -> new NotFoundException("用药计划不存在")));
    }

    @Transactional(readOnly = true)
    public MedicationAdminRecordListResponse listRecords(CurrentUser currentUser,
                                                         Long elderId,
                                                         LocalDateTime from,
                                                         LocalDateTime to,
                                                         String status,
                                                         int page,
                                                         int size) {
        Specification<MedicationAdminRecord> spec = Specification.where(null);
        if (elderId != null) {
            assertCanReadElder(currentUser, elderId);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("elderId"), elderId));
        } else {
            List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
            if (visibleElderIds != null) {
                if (visibleElderIds.isEmpty()) {
                    MedicationAdminRecordListResponse response = new MedicationAdminRecordListResponse();
                    response.setContent(List.of());
                    response.setTotalElements(0);
                    response.setPage(page);
                    response.setSize(size);
                    return response;
                }
                spec = spec.and((root, query, cb) -> root.get("elderId").in(visibleElderIds));
            }
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("administeredTime"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("administeredTime"), to));
        }
        if (status != null && !status.isBlank()) {
            String normalizedStatus = normalizeRecordStatus(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalizedStatus));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "administeredTime"));
        Page<MedicationAdminRecord> result = medicationAdminRecordRepository.findAll(spec, pageable);

        MedicationAdminRecordListResponse response = new MedicationAdminRecordListResponse();
        response.setContent(result.getContent().stream().map(this::toRecordDTO).toList());
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional
    public MedicationAdminRecordDTO createRecord(CurrentUser currentUser, CreateMedicationAdminRecordRequest request) {
        requirePlanWriter(currentUser);
        permissionService.assertCanAccessElder(currentUser, request.getElderId());

        medicationRepository.findById(request.getMedicationId())
                .orElseThrow(() -> new NotFoundException("药品不存在"));

        if (request.getPlanId() != null) {
            MedicationPlan plan = medicationPlanRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new NotFoundException("用药计划不存在"));
            if (!plan.getElderId().equals(request.getElderId())) {
                throw badRequest("planId与elderId不匹配");
            }
            if (!planReferencesMedication(plan, request.getMedicationId())) {
                throw badRequest("planId与medicationId不匹配");
            }
        }

        String normalizedStatus = normalizeRecordStatus(request.getStatus());
        LocalDateTime now = LocalDateTime.now();

        MedicationAdminRecord record = new MedicationAdminRecord();
        record.setElderId(request.getElderId());
        record.setMedicationId(request.getMedicationId());
        record.setPlanId(request.getPlanId());
        record.setAdministeredTime(request.getAdministeredTime() == null ? now : request.getAdministeredTime());
        record.setAdministeredBy(currentUser.getUserId());
        record.setStatus(normalizedStatus);
        record.setDosage(request.getDosage());
        record.setNote(request.getNote());
        record.setCreatedAt(now);

        MedicationAdminRecord saved = medicationAdminRecordRepository.save(record);
        syncPlanTaskAfterAdminRecord(saved, currentUser.getUserId());
        return toRecordDTO(saved);
    }

    private void createMedicationTasksForPlan(MedicationPlan plan, Long actorId) {
        List<PlanMedicationItem> items = parsePlanItems(plan);
        if (items.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate horizonEnd = today.plusDays(1);
        LocalDate fromDate = plan.getStartDate().isAfter(today) ? plan.getStartDate() : today;
        LocalDate endDate = plan.getEndDate() == null ? horizonEnd : (plan.getEndDate().isBefore(horizonEnd) ? plan.getEndDate() : horizonEnd);

        if (fromDate.isAfter(endDate)) {
            return;
        }

        List<Long> nurses = careTeamAssignmentRepository.findActiveNurseIdsByElderId(plan.getElderId());
        Long assignedTo = nurses.isEmpty() ? null : nurses.get(0);

        LocalDate date = fromDate;
        while (!date.isAfter(endDate)) {
            for (PlanMedicationItem item : items) {
                for (String time : item.getTimes()) {
                    LocalTime lt = LocalTime.parse(time, HH_MM);
                    LocalDateTime dueAt = LocalDateTime.of(date, lt);
                    if (dueAt.isBefore(LocalDateTime.now().minusMinutes(1))) {
                        continue;
                    }
                    Task task = new Task();
                    task.setElderId(plan.getElderId());
                    task.setTaskType("medication");
                    task.setTitle("给药：" + item.getMedicationName() + " " + item.getDosage() + "（" + time + "）");
                    task.setDescription("频次: " + item.getFrequency());
                    task.setPriority("high");
                    task.setStatus("pending");
                    task.setDueAt(dueAt);
                    task.setAssignedTo(assignedTo);
                    task.setCreatedBy(actorId);
                    task.setRelatedBizType("med_plan");
                    task.setRelatedBizId(plan.getPlanId());
                    task.setCreatedAt(LocalDateTime.now());
                    task.setUpdatedAt(LocalDateTime.now());
                    taskRepository.save(task);
                }
            }
            date = date.plusDays(1);
        }
    }

    private void syncPlanTaskAfterAdminRecord(MedicationAdminRecord record, Long operatorId) {
        if (record.getPlanId() == null) {
            return;
        }
        if (!("given".equals(record.getStatus()) || "refused".equals(record.getStatus()) || "missed".equals(record.getStatus()))) {
            return;
        }

        List<Task> candidates = taskRepository.findByRelatedBizTypeAndRelatedBizIdAndTaskTypeAndStatusIn(
                "med_plan",
                record.getPlanId(),
                "medication",
                List.of("pending", "in_progress", "overdue")
        );
        if (candidates.isEmpty()) {
            return;
        }

        Task nearest = null;
        long minSeconds = Long.MAX_VALUE;
        for (Task task : candidates) {
            LocalDateTime dueAt = task.getDueAt();
            long diff;
            if (dueAt == null) {
                diff = Long.MAX_VALUE - 1;
            } else {
                diff = Math.abs(ChronoUnit.SECONDS.between(dueAt, record.getAdministeredTime()));
            }
            if (diff < minSeconds) {
                minSeconds = diff;
                nearest = task;
            }
        }

        if (nearest == null) {
            return;
        }

        if ("given".equals(record.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            taskRepository.completeIfMatch(nearest.getTaskId(), List.of("pending", "in_progress", "overdue"), operatorId, now, now);
            return;
        }

        taskRepository.updateStatusIfMatch(nearest.getTaskId(), List.of("pending", "in_progress", "overdue"), "cancelled", LocalDateTime.now());
    }

    private MedicationPlanDTO toPlanDTO(MedicationPlan plan) {
        MedicationPlanDTO dto = new MedicationPlanDTO();
        List<PlanMedicationItem> items = parsePlanItems(plan);
        dto.setPlanId(plan.getPlanId());
        dto.setElderId(plan.getElderId());
        dto.setMedicationItems(items.stream().map(this::toMedicationPlanItemDTO).toList());
        if (!items.isEmpty()) {
            PlanMedicationItem first = items.get(0);
            dto.setMedicationId(first.getMedicationId());
            dto.setDosage(first.getDosage());
            dto.setFrequency(first.getFrequency());
            dto.setTimes(first.getTimes());
        } else {
            dto.setMedicationId(plan.getMedicationId());
            dto.setDosage(plan.getDosage());
            dto.setFrequency(plan.getFrequency());
            dto.setTimes(parseTimesJson(plan.getTimesJson()));
        }
        dto.setStartDate(plan.getStartDate());
        dto.setEndDate(plan.getEndDate());
        dto.setStatus(plan.getStatus());
        dto.setCreatedBy(plan.getCreatedBy());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());
        return dto;
    }

    private MedicationAdminRecordDTO toRecordDTO(MedicationAdminRecord record) {
        MedicationAdminRecordDTO dto = new MedicationAdminRecordDTO();
        dto.setRecordId(record.getRecordId());
        dto.setElderId(record.getElderId());
        dto.setMedicationId(record.getMedicationId());
        dto.setPlanId(record.getPlanId());
        dto.setAdministeredTime(record.getAdministeredTime());
        dto.setAdministeredBy(record.getAdministeredBy());
        dto.setStatus(record.getStatus());
        dto.setDosage(record.getDosage());
        dto.setNote(record.getNote());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw badRequest("endDate不能早于startDate");
        }
    }

    private List<String> validateAndNormalizeTimes(List<String> times) {
        List<String> normalized = new ArrayList<>();
        for (String time : times) {
            if (time == null || time.isBlank()) {
                throw badRequest("times包含空值");
            }
            try {
                LocalTime parsed = LocalTime.parse(time.trim(), HH_MM);
                normalized.add(parsed.format(HH_MM));
            } catch (DateTimeParseException ex) {
                throw badRequest("times格式错误，应为HH:mm");
            }
        }
        return normalized;
    }

    private List<PlanMedicationItem> validateAndBuildPlanItems(List<MedicationPlanItemRequest> medicationItems,
                                                               Long legacyMedicationId,
                                                               String legacyDosage,
                                                               String legacyFrequency,
                                                               List<String> legacyTimes) {
        List<MedicationPlanItemRequest> sourceItems = medicationItems;
        if (sourceItems == null || sourceItems.isEmpty()) {
            if (legacyMedicationId == null || legacyDosage == null || legacyFrequency == null
                    || legacyTimes == null || legacyTimes.isEmpty()) {
                throw badRequest("medicationItems不能为空");
            }
            MedicationPlanItemRequest legacy = new MedicationPlanItemRequest();
            legacy.setMedicationId(legacyMedicationId);
            legacy.setDosage(legacyDosage);
            legacy.setFrequency(legacyFrequency);
            legacy.setTimes(legacyTimes);
            sourceItems = List.of(legacy);
        }

        List<PlanMedicationItem> normalized = new ArrayList<>();
        for (MedicationPlanItemRequest item : sourceItems) {
            Medication medication = medicationRepository.findById(item.getMedicationId())
                    .orElseThrow(() -> new NotFoundException("药品不存在"));
            PlanMedicationItem normalizedItem = new PlanMedicationItem();
            normalizedItem.setMedicationId(item.getMedicationId());
            normalizedItem.setMedicationName(medication.getMedicationName());
            normalizedItem.setDosage(item.getDosage().trim());
            normalizedItem.setFrequency(normalizeSimple(item.getFrequency()));
            normalizedItem.setTimes(validateAndNormalizeTimes(item.getTimes()));
            normalized.add(normalizedItem);
        }
        return normalized;
    }

    private void applyPlanItems(MedicationPlan plan, List<PlanMedicationItem> items) {
        PlanMedicationItem first = items.get(0);
        plan.setMedicationId(first.getMedicationId());
        plan.setDosage(first.getDosage());
        plan.setFrequency(first.getFrequency());
        plan.setTimesJson(writeJson(first.getTimes()));
        plan.setMedicationsJson(writeJson(items));
    }

    private void validatePlanTransition(String from, String to) {
        if (from.equals(to)) {
            throw badRequest("from和to不能相同");
        }
        boolean valid = ("active".equals(from) && ("paused".equals(to) || "ended".equals(to)))
                || ("paused".equals(from) && ("active".equals(to) || "ended".equals(to)));
        if (!valid) {
            throw badRequest("不支持的状态迁移: " + from + " -> " + to);
        }
    }

    private String normalizeStatus(String status) {
        String normalized = normalizeSimple(status);
        if (!PLAN_STATUS.contains(normalized)) {
            throw badRequest("非法计划状态: " + status);
        }
        return normalized;
    }

    private String normalizeRecordStatus(String status) {
        String normalized = normalizeSimple(status);
        if (!RECORD_STATUS.contains(normalized)) {
            throw badRequest("非法给药记录状态: " + status);
        }
        return normalized;
    }

    private String normalizeSimple(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void requireCatalogManager(CurrentUser currentUser) {
        if (currentUser.hasRole("admin")) {
            return;
        }
        throw new AccessDeniedException("仅管理员可维护药品库");
    }

    private void requirePlanWriter(CurrentUser currentUser) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            return;
        }
        throw new AccessDeniedException("当前角色无权限维护用药计划/给药记录");
    }

    private void assertCanReadElder(CurrentUser currentUser, Long elderId) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")
                || currentUser.hasRole("family") || currentUser.hasRole("elder")) {
            permissionService.assertCanAccessElder(currentUser, elderId);
            return;
        }
        throw new AccessDeniedException("当前角色无权限访问用药数据");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw badRequest("JSON序列化失败");
        }
    }

    private List<String> parseTimesJson(String timesJson) {
        if (timesJson == null || timesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(timesJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw badRequest("times_json解析失败");
        }
    }

    private List<PlanMedicationItem> parsePlanItems(MedicationPlan plan) {
        String medicationsJson = plan.getMedicationsJson();
        if (medicationsJson != null && !medicationsJson.isBlank()) {
            try {
                return objectMapper.readValue(medicationsJson, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw badRequest("medications_json解析失败");
            }
        }

        if (plan.getMedicationId() == null) {
            return List.of();
        }
        PlanMedicationItem legacy = new PlanMedicationItem();
        legacy.setMedicationId(plan.getMedicationId());
        legacy.setMedicationName(resolveMedicationName(plan.getMedicationId()));
        legacy.setDosage(plan.getDosage());
        legacy.setFrequency(plan.getFrequency());
        legacy.setTimes(parseTimesJson(plan.getTimesJson()));
        return List.of(legacy);
    }

    private MedicationPlanItemDTO toMedicationPlanItemDTO(PlanMedicationItem item) {
        MedicationPlanItemDTO dto = new MedicationPlanItemDTO();
        dto.setMedicationId(item.getMedicationId());
        dto.setMedicationName(item.getMedicationName());
        dto.setDosage(item.getDosage());
        dto.setFrequency(item.getFrequency());
        dto.setTimes(item.getTimes());
        return dto;
    }

    private boolean planReferencesMedication(MedicationPlan plan, Long medicationId) {
        return parsePlanItems(plan).stream().anyMatch(item -> medicationId.equals(item.getMedicationId()));
    }

    private String resolveMedicationName(Long medicationId) {
        return medicationRepository.findById(medicationId)
                .map(Medication::getMedicationName)
                .orElse(String.valueOf(medicationId));
    }

    private static class PlanMedicationItem {
        private Long medicationId;
        private String medicationName;
        private String dosage;
        private String frequency;
        private List<String> times;

        public Long getMedicationId() {
            return medicationId;
        }

        public void setMedicationId(Long medicationId) {
            this.medicationId = medicationId;
        }

        public String getMedicationName() {
            return medicationName;
        }

        public void setMedicationName(String medicationName) {
            this.medicationName = medicationName;
        }

        public String getDosage() {
            return dosage;
        }

        public void setDosage(String dosage) {
            this.dosage = dosage;
        }

        public String getFrequency() {
            return frequency;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }

        public List<String> getTimes() {
            return times;
        }

        public void setTimes(List<String> times) {
            this.times = times;
        }
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }
}
