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
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.*;
import com.wanghao.eldercare.eldercaresystem.service.audit.AuditService;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final CarePlanRepository carePlanRepository;
    private final CarePlanChangeRepository carePlanChangeRepository;
    private final CarePlanTaskGenerator carePlanTaskGenerator;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public CarePlanService(CarePlanRepository carePlanRepository,
                           CarePlanChangeRepository carePlanChangeRepository,
                           CarePlanTaskGenerator carePlanTaskGenerator,
                           PermissionService permissionService,
                           ObjectMapper objectMapper,
                           AuditService auditService) {
        this.carePlanRepository = carePlanRepository;
        this.carePlanChangeRepository = carePlanChangeRepository;
        this.carePlanTaskGenerator = carePlanTaskGenerator;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CarePlanDTO> listCarePlans(CurrentUser currentUser, Long elderId, String status) {
        if (!canRead(currentUser)) {
            throw new AccessDeniedException("当前角色无权限查看护理计划");
        }

        Specification<CarePlan> spec = Specification.where(null);
        if (elderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("elderId"), elderId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.toLowerCase(Locale.ROOT)));
        }

        List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
        if (visibleElderIds != null && visibleElderIds.isEmpty()) {
            return List.of();
        }
        if (visibleElderIds != null) {
            spec = spec.and((root, query, cb) -> root.get("elderId").in(visibleElderIds));
        }

        return carePlanRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "version"))
                .stream()
                .map(CarePlanDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CarePlanDTO getCarePlan(CurrentUser currentUser, Long carePlanId) {
        if (!canRead(currentUser)) {
            throw new AccessDeniedException("当前角色无权限查看护理计划");
        }
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        permissionService.assertCanAccessElder(currentUser, plan.getElderId());
        return CarePlanDTO.from(plan);
    }

    @Transactional
    public CarePlanDTO createCarePlan(CurrentUser currentUser, UpsertCarePlanRequest request) {
        requireCarePlanManager(currentUser);
        Integer version = resolveCreateVersion(request.getElderId(), request.getVersion());
        String status = normalizeStatus(request.getStatus());
        validateDateRange(resolveStartDate(request), request.getEndDate());

        CarePlan plan = new CarePlan();
        plan.setElderId(request.getElderId());
        plan.setVersion(version);
        plan.setStatus(status);
        applyUpsertFields(plan, request);
        plan.setCreatedBy(currentUser.getUserId());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        if (request.getApprovedBy() != null || request.getApprovedAt() != null) {
            plan.setApprovedBy(request.getApprovedBy());
            plan.setApprovedAt(request.getApprovedAt() == null ? LocalDateTime.now() : request.getApprovedAt());
        }
        return CarePlanDTO.from(carePlanRepository.save(plan));
    }

    @Transactional
    public CarePlanDTO updateCarePlan(CurrentUser currentUser, Long carePlanId, UpsertCarePlanRequest request) {
        requireCarePlanManager(currentUser);
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));

        Integer targetVersion = request.getVersion() == null ? plan.getVersion() : request.getVersion();
        if (targetVersion == null || targetVersion <= 0) {
            throw badRequest("version 必须是正整数");
        }
        if (carePlanRepository.existsByElderIdAndVersionAndCarePlanIdNot(request.getElderId(), targetVersion, carePlanId)) {
            throw badRequest("同一老人下 version 已存在");
        }
        String status = normalizeStatus(request.getStatus());
        validateDateRange(resolveStartDate(request), request.getEndDate());

        plan.setElderId(request.getElderId());
        plan.setVersion(targetVersion);
        plan.setStatus(status);
        applyUpsertFields(plan, request);
        if (request.getApprovedBy() != null || request.getApprovedAt() != null) {
            plan.setApprovedBy(request.getApprovedBy());
            plan.setApprovedAt(request.getApprovedAt() == null ? LocalDateTime.now() : request.getApprovedAt());
        }
        plan.setUpdatedAt(LocalDateTime.now());
        return CarePlanDTO.from(carePlanRepository.save(plan));
    }

    @Transactional
    public void deleteCarePlan(CurrentUser currentUser, Long carePlanId) {
        requireCarePlanManager(currentUser);
        CarePlan plan = carePlanRepository.findById(carePlanId)
                .orElseThrow(() -> new NotFoundException("护理计划不存在"));
        carePlanRepository.delete(plan);
    }

    @Transactional
    public IdResponse createChange(CurrentUser currentUser, CreateCarePlanChangeRequest request) {
        if (!(currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")
                || currentUser.hasRole("nurse_leader") || currentUser.hasRole("admin"))) {
            throw new AccessDeniedException("当前角色无权限发起护理计划变更");
        }

        permissionService.assertCanAccessElder(currentUser, request.getElderId());

        CarePlanChangeRequest entity = new CarePlanChangeRequest();
        entity.setElderId(request.getElderId());
        entity.setFromCarePlanId(carePlanRepository.findByElderIdAndStatus(request.getElderId(), "active")
                .map(CarePlan::getCarePlanId)
                .orElse(null));
        entity.setChangeType("other");
        entity.setRequestedBy(currentUser.getUserId());
        entity.setStatus("pending");
        entity.setReason(request.getReason());
        entity.setProposedJson(buildProposedJson(request.getProposedTitle(), request.getProposedContent()));
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

        List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
        if (visibleElderIds != null && visibleElderIds.isEmpty()) {
            return emptyPage(page, size);
        }
        if (visibleElderIds != null) {
            spec = spec.and((root, query, cb) -> root.get("elderId").in(visibleElderIds));
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
        permissionService.assertCanAccessElder(currentUser, entity.getElderId());
        return CarePlanChangeDTO.from(entity);
    }

    @Transactional
    public CarePlanChangeDTO approve(CurrentUser currentUser, Long id, ReviewCarePlanChangeRequest request) {
        if (!(currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader"))) {
            throw new AccessDeniedException("仅管理员/护士长可审批");
        }

        CarePlanChangeRequest change = getChangeOrThrow(id);
        permissionService.assertCanAccessElder(currentUser, change.getElderId());

        LocalDateTime now = LocalDateTime.now();
        String reviewComment = request == null ? null : request.getComment();
        int updated = carePlanChangeRepository.approveIfPending(id, currentUser.getUserId(), now, reviewComment, now);
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 pending -> approved");
        }

        Integer maxVersion = carePlanRepository.findMaxVersionByElderId(change.getElderId());
        int nextVersion = (maxVersion == null ? 0 : maxVersion) + 1;

        CarePlan activeBefore = carePlanRepository.findByElderIdAndStatus(change.getElderId(), "active").orElse(null);
        int deactivated = carePlanRepository.deactivateActiveByElderId(change.getElderId(), now);

        CarePlan newPlan = new CarePlan();
        newPlan.setElderId(change.getElderId());
        newPlan.setVersion(nextVersion);
        newPlan.setStatus("active");
        String proposedJson = change.getProposedJson();
        newPlan.setPlanTitle(resolveProposedTitle(proposedJson));
        newPlan.setPlanContentJson(proposedJson);
        newPlan.setEffectiveDate(resolveEffectiveDate(proposedJson, LocalDate.now()));
        newPlan.setCreatedBy(currentUser.getUserId());
        newPlan.setCreatedAt(now);
        newPlan.setUpdatedAt(now);
        CarePlan savedPlan = carePlanRepository.save(newPlan);

        TaskRegenerationResult taskResult = carePlanTaskGenerator.regenerate(
                savedPlan,
                currentUser.getUserId(),
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

        CarePlanChangeDTO dto = CarePlanChangeDTO.from(getChangeOrThrow(id));
        dto.setNewPlanId(savedPlan.getCarePlanId());
        dto.setGeneratedTaskCount(taskResult.getGeneratedCount());
        return dto;
    }

    @Transactional
    public CarePlanChangeDTO reject(CurrentUser currentUser, Long id, ReviewCarePlanChangeRequest request) {
        if (!(currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader"))) {
            throw new AccessDeniedException("仅管理员/护士长可审批");
        }

        CarePlanChangeRequest change = getChangeOrThrow(id);
        permissionService.assertCanAccessElder(currentUser, change.getElderId());

        String rejectReason = request == null ? null : request.getComment();
        LocalDateTime now = LocalDateTime.now();
        int updated = carePlanChangeRepository.rejectIfPending(id, currentUser.getUserId(), now, rejectReason, now);
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 pending -> rejected");
        }

        return CarePlanChangeDTO.from(getChangeOrThrow(id));
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

    private String buildProposedJson(String proposedTitle, JsonNode proposedContent) {
        try {
            JsonNode content = proposedContent == null ? objectMapper.createObjectNode() : proposedContent.deepCopy();
            if (!content.isObject()) {
                return toJson(content);
            }
            ((com.fasterxml.jackson.databind.node.ObjectNode) content).put("proposedTitle", proposedTitle);
            return toJson(content);
        } catch (Exception e) {
            return toJson(proposedContent);
        }
    }

    private String resolveProposedTitle(String proposedJson) {
        if (proposedJson == null || proposedJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(proposedJson);
            String title = root.path("proposedTitle").asText(null);
            if (title == null || title.isBlank()) {
                title = root.path("title").asText(null);
            }
            return title;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean canRead(CurrentUser currentUser) {
        return currentUser.hasRole("admin")
                || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("nurse")
                || currentUser.hasRole("caregiver")
                || currentUser.hasRole("family")
                || currentUser.hasRole("elder");
    }

    private void requireCarePlanManager(CurrentUser currentUser) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            return;
        }
        throw new AccessDeniedException("仅管理员/护士长可直接维护护理计划");
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

    private String normalizeStatus(String status) {
        String normalized = status == null ? "active" : status.trim().toLowerCase(Locale.ROOT);
        if (!DIRECT_PLAN_STATUSES.contains(normalized)) {
            throw badRequest("非法状态，仅支持 draft/active/inactive/archived");
        }
        return normalized;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw badRequest("endDate 不能早于 startDate");
        }
    }

    private LocalDate resolveStartDate(UpsertCarePlanRequest request) {
        return request.getStartDate() != null ? request.getStartDate() : request.getEffectiveDate();
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
        plan.setCareTime(careTime);
        plan.setCareContent(careContent);
        plan.setMedicationReminder(request.getMedicationReminder());
        plan.setDietPlan(request.getDietPlan());
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

    private CarePlanChangeListResponse emptyPage(int page, int size) {
        CarePlanChangeListResponse response = new CarePlanChangeListResponse();
        response.setContent(List.of());
        response.setTotalElements(0);
        response.setPage(page);
        response.setSize(size);
        return response;
    }
}
