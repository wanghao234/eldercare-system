package com.wanghao.eldercare.eldercaresystem.service.careplan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careplan.*;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.AdmissionRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.audit.AuditService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
public class CarePlanService {
    private static final Set<String> DIRECT_PLAN_STATUSES = Set.of("draft", "active", "inactive", "archived");
    private static final List<String> UNFINISHED_TASK_STATUSES = List.of("pending", "in_progress", "overdue");

    private final CarePlanRepository carePlanRepository;
    private final CarePlanChangeRepository carePlanChangeRepository;
    private final TaskRepository taskRepository;
    private final CarePlanTaskGenerator carePlanTaskGenerator;
    private final CarePlanTaskService carePlanTaskService;
    private final AdmissionRecordRepository admissionRecordRepository;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public CarePlanService(CarePlanRepository carePlanRepository,
                           CarePlanChangeRepository carePlanChangeRepository,
                           TaskRepository taskRepository,
                           CarePlanTaskGenerator carePlanTaskGenerator,
                           CarePlanTaskService carePlanTaskService,
                           AdmissionRecordRepository admissionRecordRepository,
                           PermissionService permissionService,
                           ObjectMapper objectMapper,
                           AuditService auditService,
                           UserRepository userRepository) {
        this.carePlanRepository = carePlanRepository;
        this.carePlanChangeRepository = carePlanChangeRepository;
        this.taskRepository = taskRepository;
        this.carePlanTaskGenerator = carePlanTaskGenerator;
        this.carePlanTaskService = carePlanTaskService;
        this.admissionRecordRepository = admissionRecordRepository;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CarePlanListResponse listCarePlans(CurrentUser currentUser,
                                              Long elderId,
                                              String status,
                                              int page,
                                              int size) {
        if (!canRead(currentUser)) {
            throw new AccessDeniedException("当前角色无权限查看护理计划");
        }

        Page<User> elderPage = searchVisibleElders(currentUser, elderId, page, size);
        List<User> elders = elderPage.getContent();
        List<Long> elderIds = elders.stream().map(User::getUserId).toList();

        Map<Long, AdmissionRecord> activeAdmissionMap = loadActiveAdmissionMap(elderIds);
        Map<Long, CarePlan> latestPlanMap = loadLatestPlanMap(elderIds, status);

        List<CarePlanListItemDTO> items = new ArrayList<>();
        for (User elder : elders) {
            CarePlan plan = latestPlanMap.get(elder.getUserId());
            CarePlanListItemDTO item = new CarePlanListItemDTO();
            item.setElderId(elder.getUserId());
            item.setElderUsername(trimToNull(elder.getUsername()));
            item.setElderName(trimToNull(elder.getRealName()));
            item.setElderStatus(trimToNull(elder.getStatus()));
            AdmissionRecord admission = activeAdmissionMap.get(elder.getUserId());
            boolean inResidence = admission != null && "active".equalsIgnoreCase(admission.getStatus());
            item.setAdmissionStatus(admission == null ? null : trimToNull(admission.getStatus()));
            item.setInResidence(inResidence);
            if (plan != null) {
                item.setHasCarePlan(true);
                item.setCarePlanId(plan.getCarePlanId());
                item.setCarePlanVersion(plan.getVersion());
                item.setCarePlanStatus(plan.getStatus());
                item.setCareTime(trimToNull(plan.getCareTime()));
                item.setCareLevel(trimToNull(plan.getCareLevel()));
                item.setDailyCare(firstNonBlank(trimToNull(plan.getDailyCare()), trimToNull(plan.getCareContent())));
                item.setMedicationCare(firstNonBlank(trimToNull(plan.getMedicationCare()), trimToNull(plan.getMedicationReminder())));
                item.setExecutionFrequency(firstNonBlank(trimToNull(plan.getExecutionFrequency()), trimToNull(plan.getCareTime())));
                item.setDietPlan(trimToNull(plan.getDietPlan()));
                item.setStartDate(plan.getStartDate());
                item.setEndDate(plan.getEndDate());
                item.setPlanUpdatedAt(plan.getUpdatedAt());
            }
            item.setCanCreateCarePlan(inResidence);
            item.setCanSubmitCarePlan(false);
            item.setCanChangeCarePlan(inResidence && plan != null);
            items.add(item);
        }

        CarePlanListResponse response = new CarePlanListResponse();
        response.setContent(items);
        response.setTotalElements(elderPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public CarePlanDTO getCarePlan(CurrentUser currentUser, Long carePlanId) {
        if (!canRead(currentUser)) {
            throw new AccessDeniedException("当前角色无权限查看护理计划");
        }
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        if (!currentUser.hasRole("doctor")) {
            permissionService.assertCanAccessElder(currentUser, plan.getElderId());
        }
        return CarePlanDTO.from(plan);
    }

    @Transactional
    public CarePlanDTO createCarePlan(CurrentUser currentUser, UpsertCarePlanRequest request) {
        requireCarePlanDraftManager(currentUser);
        permissionService.assertCanAccessElder(currentUser, request.getElderId());
        assertActiveAdmission(request.getElderId());
        Integer version = resolveCreateVersion(request.getElderId(), request.getVersion());
        validateDateRange(resolveStartDate(request), request.getEndDate());
        LocalDateTime now = LocalDateTime.now();
        carePlanRepository.deactivateActiveByElderId(request.getElderId(), now);

        CarePlan plan = new CarePlan();
        plan.setElderId(request.getElderId());
        plan.setVersion(version);
        plan.setStatus("active");
        plan.setApprovedBy(currentUser.getUserId());
        plan.setApprovedAt(now);
        applyUpsertFields(plan, request);
        plan.setCreatedBy(currentUser.getUserId());
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        CarePlan saved = carePlanRepository.save(plan);
        carePlanTaskGenerator.regenerate(saved, currentUser.getUserId(), 7, null);
        CarePlanDTO dto = CarePlanDTO.from(saved);
        applyAutoTaskGenerationResult(dto, carePlanTaskService.autoGenerateTasksAfterCarePlanSaved(currentUser, saved));
        return dto;
    }

    @Transactional
    public CarePlanDTO updateCarePlan(CurrentUser currentUser, Long carePlanId, UpsertCarePlanRequest request) {
        requireCarePlanDraftManager(currentUser);
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());
        assertActiveAdmission(plan.getElderId());
        if (!plan.getElderId().equals(request.getElderId())) {
            assertActiveAdmission(request.getElderId());
        }

        Integer targetVersion = request.getVersion() == null ? plan.getVersion() : request.getVersion();
        if (targetVersion == null || targetVersion <= 0) {
            throw badRequest("version 必须是正整数");
        }
        if (carePlanRepository.existsByElderIdAndVersionAndCarePlanIdNot(request.getElderId(), targetVersion, carePlanId)) {
            throw badRequest("同一老人下 version 已存在");
        }
        permissionService.assertCanAccessElder(currentUser, request.getElderId());
        validateDateRange(resolveStartDate(request), request.getEndDate());
        LocalDateTime now = LocalDateTime.now();
        carePlanRepository.deactivateActiveByElderId(request.getElderId(), now);

        plan.setElderId(request.getElderId());
        plan.setVersion(targetVersion);
        plan.setStatus("active");
        plan.setApprovedBy(currentUser.getUserId());
        plan.setApprovedAt(now);
        applyUpsertFields(plan, request);
        plan.setUpdatedAt(now);
        CarePlan saved = carePlanRepository.save(plan);
        carePlanTaskGenerator.regenerate(saved, currentUser.getUserId(), 7, null);
        CarePlanDTO dto = CarePlanDTO.from(saved);
        applyAutoTaskGenerationResult(dto, carePlanTaskService.autoGenerateTasksAfterCarePlanSaved(currentUser, saved));
        return dto;
    }

    @Transactional
    public void deleteCarePlan(CurrentUser currentUser, Long carePlanId) {
        requireCarePlanDraftManager(currentUser);
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());
        LocalDateTime now = LocalDateTime.now();
        taskRepository.deleteFutureByBizAndStatuses(
                "care_plan",
                plan.getCarePlanId(),
                UNFINISHED_TASK_STATUSES,
                now
        );
        carePlanTaskService.softDeleteTasksByCarePlanId(plan.getCarePlanId());
        plan.setStatus("archived");
        plan.setUpdatedAt(now);
        carePlanRepository.save(plan);
    }

    @Transactional
    public IdResponse createChange(CurrentUser currentUser, CreateCarePlanChangeRequest request) {
        requireCarePlanChangeInitiator(currentUser);
        assertActiveAdmission(request.getElderId());

        CarePlan draftPlan = null;
        CarePlan activePlan = carePlanRepository.findByElderIdAndStatus(request.getElderId(), "active").orElse(null);
        if (request.getDraftPlanId() != null) {
            draftPlan = carePlanRepository.findById(request.getDraftPlanId())
                    .orElseThrow(() -> new NotFoundException("护理计划草稿不存在"));
            ensureDraftEditable(draftPlan);
            if (!draftPlan.getElderId().equals(request.getElderId())) {
                throw badRequest("draftPlanId 与 elderId 不匹配");
            }
            permissionService.assertCanAccessElder(currentUser, draftPlan.getElderId());
        } else {
            permissionService.assertCanAccessElder(currentUser, request.getElderId());
        }
        ensureNoOpenChange(request.getElderId());

        boolean hasExistingPlan = activePlan != null;
        if (!hasExistingPlan && draftPlan == null) {
            throw badRequest("老人当前无生效护理计划，提交初次审批时必须选择护理计划草稿");
        }

        CarePlanChangeRequest entity = new CarePlanChangeRequest();
        entity.setElderId(request.getElderId());
        entity.setFromCarePlanId(hasExistingPlan ? activePlan.getCarePlanId() : null);
        String changeType = normalizeChangeType(request.getChangeType(), request.getRequiresDoctorReview(), hasExistingPlan);
        entity.setChangeType(changeType);
        entity.setRequestedBy(currentUser.getUserId());
        entity.setStatus("pending");
        entity.setReason(request.getReason());
        entity.setProposedJson(buildProposedJson(request, draftPlan));
        entity.setEvidenceJson(buildEvidenceJson(request.getDraftPlanId(), changeType, requiresDoctorReview(changeType, request.getRequiresDoctorReview())));
        entity.setRequestedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        CarePlanChangeRequest saved = carePlanChangeRepository.save(entity);
        return new IdResponse(saved.getChangeId());
    }

    @Transactional(readOnly = true)
    public CarePlanChangeListResponse listChanges(CurrentUser currentUser,
                                                  Long elderId,
                                                  String status,
                                                  int page,
                                                  int size) {
        if (!canRead(currentUser)) {
            throw new AccessDeniedException("当前角色无权限查看护理计划变更");
        }

        Specification<CarePlanChangeRequest> spec = Specification.where(null);
        if (elderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("elderId"), elderId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.toLowerCase(Locale.ROOT)));
        }

        if (!currentUser.hasRole("doctor")) {
            List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
            if (visibleElderIds != null && visibleElderIds.isEmpty()) {
                return emptyPage(page, size);
            }
            if (visibleElderIds != null) {
                spec = spec.and((root, query, cb) -> root.get("elderId").in(visibleElderIds));
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CarePlanChangeRequest> result = carePlanChangeRepository.findAll(spec, pageable);

        CarePlanChangeListResponse response = new CarePlanChangeListResponse();
        response.setContent(result.getContent().stream().map(CarePlanChangeDTO::from).toList());
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public CarePlanChangeDTO getChangeDetail(CurrentUser currentUser, Long id) {
        if (!canRead(currentUser)) {
            throw new AccessDeniedException("当前角色无权限查看护理计划变更");
        }

        CarePlanChangeRequest entity = getChangeOrThrow(id);
        if (!currentUser.hasRole("doctor")) {
            permissionService.assertCanAccessElder(currentUser, entity.getElderId());
        }
        return CarePlanChangeDTO.from(entity);
    }

    @Transactional
    public CarePlanChangeDTO approve(CurrentUser currentUser, Long id, ReviewCarePlanChangeRequest request) {
        CarePlanChangeRequest change = getChangeOrThrow(id);
        if (!(currentUser.hasRole("admin") || currentUser.hasRole("doctor"))) {
            permissionService.assertCanAccessElder(currentUser, change.getElderId());
        }
        LocalDateTime now = LocalDateTime.now();
        String reviewComment = request == null ? null : request.getComment();

        if ("pending".equalsIgnoreCase(change.getStatus())) {
            requireLeaderReviewer(currentUser);
            if (requiresDoctorReview(change)) {
                change.setStatus("doctor_review");
                change.setReviewedBy(currentUser.getUserId());
                change.setReviewedAt(now);
                change.setReviewComment(reviewComment);
                change.setUpdatedAt(now);
                return CarePlanChangeDTO.from(carePlanChangeRepository.save(change));
            }
            return finalizeApprovedChange(change, currentUser.getUserId(), reviewComment, now);
        }
        if ("doctor_review".equalsIgnoreCase(change.getStatus())) {
            requireDoctorReviewer(currentUser);
            return finalizeApprovedChange(change, currentUser.getUserId(), reviewComment, now);
        }
        throw badRequest("状态不匹配，仅允许 pending/doctor_review -> approved");
    }

    @Transactional
    public CarePlanChangeDTO reject(CurrentUser currentUser, Long id, ReviewCarePlanChangeRequest request) {
        CarePlanChangeRequest change = getChangeOrThrow(id);
        if (!(currentUser.hasRole("admin") || currentUser.hasRole("doctor"))) {
            permissionService.assertCanAccessElder(currentUser, change.getElderId());
        }

        String rejectReason = request == null ? null : request.getComment();
        LocalDateTime now = LocalDateTime.now();
        if ("pending".equalsIgnoreCase(change.getStatus())) {
            requireLeaderReviewer(currentUser);
        } else if ("doctor_review".equalsIgnoreCase(change.getStatus())) {
            requireDoctorReviewer(currentUser);
        } else {
            throw badRequest("状态不匹配，仅允许 pending/doctor_review -> rejected");
        }

        change.setStatus("rejected");
        change.setReviewedBy(currentUser.getUserId());
        change.setReviewedAt(now);
        change.setReviewComment(rejectReason);
        change.setUpdatedAt(now);
        return CarePlanChangeDTO.from(carePlanChangeRepository.save(change));
    }

    @Transactional
    public RegenerateTasksResponse regenerateTasks(CurrentUser currentUser, Long carePlanId, int days) {
        if (!(currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader"))) {
            throw new AccessDeniedException("仅管理员/护士长可重算护理计划任务");
        }
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        TaskRegenerationResult result = carePlanTaskGenerator.regenerate(plan, currentUser.getUserId(), days, null);
        auditService.logSuccess(AuditAction.UPDATE, "tasks", carePlanId,
                Map.of("carePlanId", carePlanId, "generatedTaskCount", result.getGeneratedCount(),
                        "deletedTaskCount", result.getDeletedCount(), "days", days));
        RegenerateTasksResponse response = new RegenerateTasksResponse();
        response.setCarePlanId(carePlanId);
        response.setDeletedTaskCount(result.getDeletedCount());
        response.setGeneratedTaskCount(result.getGeneratedCount());
        return response;
    }

    private CarePlanChangeRequest getChangeOrThrow(Long id) {
        return carePlanChangeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("护理计划变更申请不存在"));
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

    private String buildProposedJson(CreateCarePlanChangeRequest request, CarePlan draftPlan) {
        com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
        if (draftPlan != null) {
            root.put("carePlanId", draftPlan.getCarePlanId());
            root.put("elderId", draftPlan.getElderId());
            root.put("version", draftPlan.getVersion());
            putText(root, "planTitle", draftPlan.getPlanTitle());
            putText(root, "careTime", draftPlan.getCareTime());
            putText(root, "careContent", draftPlan.getCareContent());
            putText(root, "medicationReminder", draftPlan.getMedicationReminder());
            putText(root, "dietPlan", draftPlan.getDietPlan());
            putDate(root, "startDate", draftPlan.getStartDate());
            putDate(root, "endDate", draftPlan.getEndDate());
            putDate(root, "effectiveDate", draftPlan.getEffectiveDate());
        }
        putText(root, "proposedTitle", request.getProposedTitle());
        if (request.getProposedTitle() != null && (root.path("planTitle").isMissingNode() || root.path("planTitle").asText().isBlank())) {
            root.put("planTitle", request.getProposedTitle());
            root.put("careTime", request.getProposedTitle());
        }

        JsonNode proposedContent = request.getProposedContent();
        if (proposedContent != null && !proposedContent.isNull()) {
            root.set("proposedContent", proposedContent.deepCopy());
            if (proposedContent.isTextual()) {
                root.put("careContent", proposedContent.asText());
            } else {
                root.put("careContent", proposedContent.toString());
            }
            if (proposedContent.isObject()) {
                copyText(proposedContent, root, "planTitle", "careTime", "careContent", "medicationReminder", "dietPlan", "title");
                copyDate(proposedContent, root, "startDate", "endDate", "effectiveDate");
            }
        }
        return toJson(root);
    }

    private String buildEvidenceJson(Long draftPlanId, String changeType, boolean requiresDoctorReview) {
        com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
        if (draftPlanId != null) {
            root.put("draftPlanId", draftPlanId);
        }
        root.put("changeType", changeType);
        root.put("requiresDoctorReview", requiresDoctorReview);
        return toJson(root);
    }

    private String normalizeChangeType(String changeType, Boolean requiresDoctorReview, boolean hasExistingPlan) {
        if (!hasExistingPlan) {
            return "initial";
        }
        String normalized = changeType == null || changeType.isBlank() ? "routine" : changeType.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("routine", "special", "initial").contains(normalized)) {
            throw badRequest("changeType 仅支持 routine/special/initial");
        }
        if ("initial".equals(normalized)) {
            throw badRequest("老人已有生效护理计划，请提交变更审批");
        }
        if (Boolean.TRUE.equals(requiresDoctorReview) && "routine".equals(normalized)) {
            return "special";
        }
        return normalized;
    }

    private boolean requiresDoctorReview(String changeType, Boolean requestedFlag) {
        return Boolean.TRUE.equals(requestedFlag) || "special".equalsIgnoreCase(changeType);
    }

    private boolean requiresDoctorReview(CarePlanChangeRequest change) {
        if ("special".equalsIgnoreCase(change.getChangeType())) {
            return true;
        }
        JsonNode evidence = parseJson(change.getEvidenceJson());
        return evidence != null && evidence.path("requiresDoctorReview").asBoolean(false);
    }

    private Long resolveDraftPlanId(CarePlanChangeRequest change) {
        JsonNode evidence = parseJson(change.getEvidenceJson());
        if (evidence == null || evidence.path("draftPlanId").isMissingNode() || evidence.path("draftPlanId").isNull()) {
            return null;
        }
        return evidence.path("draftPlanId").asLong();
    }

    private CarePlanChangeDTO finalizeApprovedChange(CarePlanChangeRequest change,
                                                     Long reviewerId,
                                                     String reviewComment,
                                                     LocalDateTime now) {
        CarePlan activeBefore = carePlanRepository.findByElderIdAndStatus(change.getElderId(), "active").orElse(null);
        int deactivated = carePlanRepository.deactivateActiveByElderId(change.getElderId(), now);
        Long draftPlanId = resolveDraftPlanId(change);
        CarePlan savedPlan;
        if (draftPlanId != null) {
            CarePlan draftPlan = carePlanRepository.findById(draftPlanId)
                    .orElseThrow(() -> new NotFoundException("护理计划草稿不存在"));
            ensureDraftEditable(draftPlan);
            savedPlan = activateDraftPlan(draftPlan, change, reviewerId, now);
        } else {
            savedPlan = createApprovedPlan(change, reviewerId, now);
        }

        change.setStatus("approved");
        change.setReviewedBy(reviewerId);
        change.setReviewedAt(now);
        change.setReviewComment(reviewComment);
        change.setUpdatedAt(now);
        carePlanChangeRepository.save(change);

        TaskRegenerationResult taskResult = carePlanTaskGenerator.regenerate(
                savedPlan,
                reviewerId,
                7,
                activeBefore == null ? null : activeBefore.getCarePlanId()
        );
        auditService.logSuccess(AuditAction.CREATE, "care_plans", savedPlan.getCarePlanId(),
                Map.of("elderId", savedPlan.getElderId(), "version", savedPlan.getVersion(), "status", savedPlan.getStatus()));
        if (deactivated > 0 && activeBefore != null) {
            auditService.logTransition("care_plans", activeBefore.getCarePlanId(),
                    "active", "inactive", AuditAction.TRANSITION,
                    Map.of("elderId", change.getElderId(), "newPlanId", savedPlan.getCarePlanId()));
        }
        auditService.logSuccess(AuditAction.CREATE, "tasks", savedPlan.getCarePlanId(),
                Map.of("carePlanId", savedPlan.getCarePlanId(), "generatedTaskCount", taskResult.getGeneratedCount(),
                        "deletedTaskCount", taskResult.getDeletedCount()));

        CarePlanChangeDTO dto = CarePlanChangeDTO.from(change);
        dto.setNewPlanId(savedPlan.getCarePlanId());
        dto.setGeneratedTaskCount(taskResult.getGeneratedCount());
        return dto;
    }

    private CarePlan activateDraftPlan(CarePlan draftPlan,
                                       CarePlanChangeRequest change,
                                       Long reviewerId,
                                       LocalDateTime now) {
        applyProposedPlanFields(draftPlan, change.getProposedJson());
        draftPlan.setStatus("active");
        draftPlan.setApprovedBy(reviewerId);
        draftPlan.setApprovedAt(now);
        draftPlan.setUpdatedAt(now);
        return carePlanRepository.save(draftPlan);
    }

    private CarePlan createApprovedPlan(CarePlanChangeRequest change,
                                        Long reviewerId,
                                        LocalDateTime now) {
        Integer maxVersion = carePlanRepository.findMaxVersionByElderId(change.getElderId());
        int nextVersion = (maxVersion == null ? 0 : maxVersion) + 1;
        CarePlan newPlan = new CarePlan();
        newPlan.setElderId(change.getElderId());
        newPlan.setVersion(nextVersion);
        newPlan.setStatus("active");
        newPlan.setCreatedBy(change.getRequestedBy());
        newPlan.setCreatedAt(now);
        newPlan.setApprovedBy(reviewerId);
        newPlan.setApprovedAt(now);
        applyProposedPlanFields(newPlan, change.getProposedJson());
        newPlan.setUpdatedAt(now);
        return carePlanRepository.save(newPlan);
    }

    private void applyProposedPlanFields(CarePlan plan, String proposedJson) {
        JsonNode root = parseJson(proposedJson);
        if (root == null || root.isNull()) {
            return;
        }
        String careTime = firstText(root, "careTime", "planTitle", "proposedTitle", "title");
        if (careTime != null) {
            plan.setCareTime(careTime);
        }
        String careContent = resolveCareContent(root);
        if (careContent != null) {
            plan.setCareContent(careContent);
        }
        String medicationReminder = firstText(root, "medicationReminder");
        if (medicationReminder != null) {
            plan.setMedicationReminder(medicationReminder);
        }
        String dietPlan = firstText(root, "dietPlan");
        if (dietPlan != null) {
            plan.setDietPlan(dietPlan);
        }
        LocalDate startDate = firstDate(root, "startDate", "effectiveDate");
        if (startDate != null) {
            plan.setStartDate(startDate);
        }
        LocalDate endDate = firstDate(root, "endDate");
        if (endDate != null || root.has("endDate")) {
            plan.setEndDate(endDate);
        }
    }

    private String resolveCareContent(JsonNode root) {
        String direct = firstText(root, "careContent");
        if (direct != null) {
            return direct;
        }
        JsonNode proposedContent = root.path("proposedContent");
        if (proposedContent.isMissingNode() || proposedContent.isNull()) {
            return null;
        }
        return proposedContent.isTextual() ? proposedContent.asText() : proposedContent.toString();
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    private void putText(com.fasterxml.jackson.databind.node.ObjectNode root, String field, String value) {
        if (value != null && !value.isBlank()) {
            root.put(field, value);
        }
    }

    private void putDate(com.fasterxml.jackson.databind.node.ObjectNode root, String field, LocalDate value) {
        if (value != null) {
            root.put(field, value.toString());
        }
    }

    private void copyText(JsonNode source, com.fasterxml.jackson.databind.node.ObjectNode target, String... fields) {
        for (String field : fields) {
            String value = source.path(field).asText(null);
            if (value != null && !value.isBlank()) {
                target.put(field, value);
            }
        }
    }

    private void copyDate(JsonNode source, com.fasterxml.jackson.databind.node.ObjectNode target, String... fields) {
        for (String field : fields) {
            String value = source.path(field).asText(null);
            if (value != null && !value.isBlank()) {
                target.put(field, value);
            }
        }
    }

    private String firstText(JsonNode source, String... fields) {
        for (String field : fields) {
            String value = source.path(field).asText(null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private LocalDate firstDate(JsonNode source, String... fields) {
        for (String field : fields) {
            String value = source.path(field).asText(null);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                return LocalDate.parse(value);
            } catch (Exception ex) {
                throw badRequest(field + " 格式非法，需为 yyyy-MM-dd");
            }
        }
        return null;
    }

    private boolean canRead(CurrentUser currentUser) {
        return currentUser.hasRole("admin")
                || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("doctor")
                || currentUser.hasRole("nurse")
                || currentUser.hasRole("caregiver")
                || currentUser.hasRole("family")
                || currentUser.hasRole("elder");
    }

    private void requireCarePlanDraftManager(CurrentUser currentUser) {
        if (currentUser.hasRole("admin")
                || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("nurse")
                || currentUser.hasRole("caregiver")) {
            return;
        }
        throw new AccessDeniedException("仅管理员/护士长/护理员可维护护理计划草稿");
    }

    private void requireCarePlanChangeInitiator(CurrentUser currentUser) {
        if (currentUser.hasRole("admin")
                || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("nurse")
                || currentUser.hasRole("caregiver")) {
            return;
        }
        throw new AccessDeniedException("当前角色无权限发起护理计划审批");
    }

    private void requireLeaderReviewer(CurrentUser currentUser) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            return;
        }
        throw new AccessDeniedException("仅管理员/护士长可进行初审");
    }

    private void requireDoctorReviewer(CurrentUser currentUser) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("doctor")) {
            return;
        }
        throw new AccessDeniedException("仅管理员/医生可进行复审");
    }

    private void ensureDraftEditable(CarePlan plan) {
        if (!"draft".equalsIgnoreCase(plan.getStatus())) {
            throw badRequest("仅护理计划草稿可编辑或提交审批");
        }
    }

    private void assertActiveAdmission(Long elderId) {
        if (!admissionRecordRepository.existsByElderIdAndStatus(elderId, "active")) {
            throw badRequest("老人当前不在住，不能维护或提交护理计划");
        }
    }

    private void ensureNoOpenChange(Long elderId) {
        List<CarePlanChangeRequest> changes = carePlanChangeRepository.findByElderIdInOrderByCreatedAtDesc(List.of(elderId));
        for (CarePlanChangeRequest change : changes) {
            String status = change.getStatus();
            if (status != null && Set.of("pending", "doctor_review").contains(status.toLowerCase(Locale.ROOT))) {
                throw badRequest("该老人已有审批中的护理计划，请勿重复提交");
            }
        }
    }

    private Integer resolveCreateVersion(Long elderId, Integer requestedVersion) {
        Integer version = requestedVersion;
        if (version == null) {
            Integer maxVersion = carePlanRepository.findMaxVersionByElderId(elderId);
            version = (maxVersion == null ? 0 : maxVersion) + 1;
        }
        if (version <= 0) {
            throw badRequest("version 必须是正整数");
        }
        if (carePlanRepository.existsByElderIdAndVersion(elderId, version)) {
            throw badRequest("同一老人下 version 已存在");
        }
        return version;
    }

    private void applyAutoTaskGenerationResult(CarePlanDTO dto,
                                               CarePlanTaskService.TaskGenerationOutcome outcome) {
        if (dto == null || outcome == null) {
            return;
        }
        dto.setTaskGenerated(outcome.isTaskGenerated());
        dto.setGeneratedTaskCount(outcome.getGeneratedTaskCount());
        dto.setTaskGenerateMessage(outcome.getTaskGenerateMessage());
    }

    private String normalizeDraftStatus(String status) {
        String normalized = status == null || status.isBlank() ? "draft" : status.trim().toLowerCase(Locale.ROOT);
        if (!"draft".equals(normalized)) {
            throw badRequest("护理计划直接维护仅支持 draft 状态，生效需走审批");
        }
        return "draft";
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw badRequest("endDate 不能早于 startDate");
        }
    }

    private LocalDate resolveStartDate(UpsertCarePlanRequest request) {
        return request.getStartDate() != null ? request.getStartDate() : request.getEffectiveDate();
    }

    private Page<User> searchVisibleElders(CurrentUser currentUser, Long elderId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (elderId != null) {
            User elder = userRepository.findByUserIdAndDeletedAtIsNull(elderId)
                    .orElseThrow(() -> new NotFoundException("老人不存在"));
            if (!"elder".equalsIgnoreCase(elder.getRole())) {
                throw badRequest("elderId 对应的用户不是老人");
            }
            if (!canViewElderInCarePlanList(currentUser, elderId)) {
                throw new AccessDeniedException("无权限访问该老人数据");
            }
            return new org.springframework.data.domain.PageImpl<>(List.of(elder), pageable, 1);
        }

        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader") || currentUser.hasRole("doctor")) {
            return userRepository.search(null, "elder", "active", pageable);
        }

        List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
        if (visibleElderIds == null) {
            return userRepository.search(null, "elder", "active", pageable);
        }
        if (visibleElderIds.isEmpty()) {
            return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
        }
        return userRepository.searchByRoleAndIds("elder", visibleElderIds, null, "active", pageable);
    }

    private boolean canViewElderInCarePlanList(CurrentUser currentUser, Long elderId) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader") || currentUser.hasRole("doctor")) {
            return true;
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
        return visibleElderIds != null && visibleElderIds.contains(elderId);
    }

    private Map<Long, AdmissionRecord> loadActiveAdmissionMap(List<Long> elderIds) {
        if (elderIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AdmissionRecord> result = new LinkedHashMap<>();
        for (AdmissionRecord admission : admissionRecordRepository.findByElderIdInAndStatusOrderByCreatedAtDescAdmissionIdDesc(elderIds, "active")) {
            result.putIfAbsent(admission.getElderId(), admission);
        }
        return result;
    }

    private Map<Long, CarePlan> loadLatestPlanMap(List<Long> elderIds, String status) {
        if (elderIds.isEmpty()) {
            return Map.of();
        }
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toLowerCase(Locale.ROOT);
        Map<Long, CarePlan> result = new LinkedHashMap<>();
        for (CarePlan plan : carePlanRepository.findByElderIdInOrderByVersionDescUpdatedAtDesc(elderIds)) {
            if (normalizedStatus == null && "archived".equalsIgnoreCase(plan.getStatus())) {
                continue;
            }
            if (normalizedStatus != null && !normalizedStatus.equalsIgnoreCase(plan.getStatus())) {
                continue;
            }
            result.putIfAbsent(plan.getElderId(), plan);
        }
        return result;
    }

    private Map<Long, CarePlan> loadPlanMapByStatus(List<Long> elderIds, String status) {
        if (elderIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, CarePlan> result = new LinkedHashMap<>();
        for (CarePlan plan : carePlanRepository.findByElderIdInOrderByVersionDescUpdatedAtDesc(elderIds)) {
            if (!status.equalsIgnoreCase(plan.getStatus())) {
                continue;
            }
            result.putIfAbsent(plan.getElderId(), plan);
        }
        return result;
    }

    private Map<Long, CarePlanChangeRequest> loadLatestChangeMap(List<Long> elderIds, CurrentUser currentUser) {
        if (elderIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, CarePlanChangeRequest> result = new LinkedHashMap<>();
        for (CarePlanChangeRequest change : carePlanChangeRepository.findByElderIdInOrderByCreatedAtDesc(elderIds)) {
            if (currentUser.hasRole("doctor")) {
                if (!"doctor_review".equalsIgnoreCase(change.getStatus())) {
                    continue;
                }
            } else if (!Set.of("pending", "doctor_review").contains(change.getStatus().toLowerCase(Locale.ROOT))) {
                continue;
            }
            result.putIfAbsent(change.getElderId(), change);
        }
        return result;
    }

    private void applyUpsertFields(CarePlan plan, UpsertCarePlanRequest request) {
        String careTime = request.getCareTime();
        if ((careTime == null || careTime.isBlank()) && request.getPlanTitle() != null) {
            careTime = request.getPlanTitle();
        }
        String careContent = request.getCareContent();
        if ((careContent == null || careContent.isBlank()) && request.getPlanContentJson() != null) {
            careContent = request.getPlanContentJson();
        }

        plan.setStartDate(resolveStartDate(request));
        plan.setEndDate(request.getEndDate());
        plan.setCareLevel(trimToNull(request.getCareLevel()));
        plan.setCareTime(careTime);
        plan.setCareContent(careContent);
        plan.setMedicationReminder(request.getMedicationReminder());
        plan.setDietPlan(request.getDietPlan());
        plan.setHealthAssessment(trimToNull(request.getHealthAssessment()));
        plan.setNursingProblem(trimToNull(request.getNursingProblem()));
        plan.setRiskTags(trimToNull(request.getRiskTags()));
        plan.setNursingGoal(trimToNull(request.getNursingGoal()));
        plan.setDailyCare(trimToNull(request.getDailyCare()));
        plan.setMedicationCare(trimToNull(request.getMedicationCare()));
        plan.setHealthMonitoring(trimToNull(request.getHealthMonitoring()));
        plan.setRehabilitationActivity(trimToNull(request.getRehabilitationActivity()));
        plan.setPsychologicalCare(trimToNull(request.getPsychologicalCare()));
        plan.setSafetyPrecaution(trimToNull(request.getSafetyPrecaution()));
        plan.setExecutionFrequency(trimToNull(request.getExecutionFrequency()));
        plan.setEvaluation(trimToNull(request.getEvaluation()));
        plan.setAiGenerated(request.getAiGenerated() != null ? request.getAiGenerated() : Boolean.FALSE);
    }

    private LocalDate resolveEffectiveDate(String planJson, LocalDate defaultDate) {
        if (planJson == null || planJson.isBlank()) {
            return defaultDate;
        }
        try {
            JsonNode root = objectMapper.readTree(planJson);
            String startDate = root.path("startDate").asText(null);
            if (startDate != null && !startDate.isBlank()) {
                return LocalDate.parse(startDate);
            }
        } catch (Exception ignored) {
            return defaultDate;
        }
        return defaultDate;
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private CarePlanChangeListResponse emptyPage(int page, int size) {
        CarePlanChangeListResponse response = new CarePlanChangeListResponse();
        response.setContent(List.of());
        response.setTotalElements(0);
        response.setPage(page);
        response.setSize(size);
        return response;
    }
}
