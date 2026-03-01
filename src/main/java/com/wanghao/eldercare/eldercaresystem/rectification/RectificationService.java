package com.wanghao.eldercare.eldercaresystem.rectification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.alarm.Alarm;
import com.wanghao.eldercare.eldercaresystem.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.task.TaskIntegrationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RectificationService {

    private static final Set<String> TERMINAL_STATUS = Set.of("closed", "cancelled");
    private static final Set<String> VALID_STATUS = Set.of("open", "analyzing", "planning", "executing", "verifying", "closed", "cancelled");
    private static final Set<String> VALID_LEVEL = Set.of("minor", "major", "critical");
    private static final Set<String> VALID_ACTION = Set.of("analysis", "plan", "task_create", "submit", "accept", "reject", "comment");

    private final RectificationRepository rectificationRepository;
    private final RectificationActionRepository rectificationActionRepository;
    private final PermissionService permissionService;
    private final AlarmRepository alarmRepository;
    private final ObjectMapper objectMapper;
    private final TaskIntegrationService taskIntegrationService;

    public RectificationService(RectificationRepository rectificationRepository,
                                RectificationActionRepository rectificationActionRepository,
                                PermissionService permissionService,
                                AlarmRepository alarmRepository,
                                ObjectMapper objectMapper,
                                TaskIntegrationService taskIntegrationService) {
        this.rectificationRepository = rectificationRepository;
        this.rectificationActionRepository = rectificationActionRepository;
        this.permissionService = permissionService;
        this.alarmRepository = alarmRepository;
        this.objectMapper = objectMapper;
        this.taskIntegrationService = taskIntegrationService;
    }

    @Transactional
    public RectificationCreateResponse create(CurrentUser currentUser, CreateRectificationRequest request) {
        ensureRectificationModuleRole(currentUser);
        validateLevel(request.getLevel());
        assertAlarmRectificationNotDuplicated(request.getSourceType(), request.getSourceId());

        Rectification rectification = new Rectification();
        rectification.setSourceType(request.getSourceType());
        rectification.setSourceId(request.getSourceId());
        rectification.setTitle(request.getTitle());
        rectification.setDescription(request.getDescription());
        rectification.setLevel(request.getLevel().toLowerCase(Locale.ROOT));
        rectification.setOwnerId(request.getOwnerId());
        rectification.setDueAt(request.getDueAt());
        rectification.setStatus("open");
        rectification.setCreatedBy(currentUser.getUserId());
        rectification.setCreatedAt(LocalDateTime.now());
        rectification.setUpdatedAt(LocalDateTime.now());

        Rectification saved = rectificationRepository.save(rectification);
        saveAction(saved.getRectificationId(), "comment", currentUser.getUserId(), "创建整改", null, null);
        return new RectificationCreateResponse(saved.getRectificationId());
    }

    @Transactional(readOnly = true)
    public RectificationListResponse list(CurrentUser currentUser,
                                          String status,
                                          String level,
                                          Long ownerId,
                                          String sourceType,
                                          Long sourceId,
                                          int page,
                                          int size) {
        ensureRectificationModuleRole(currentUser);

        Specification<Rectification> spec = Specification.where(null);

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (level != null && !level.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("level"), level));
        }
        if (ownerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerId"), ownerId));
        }
        if (sourceType != null && !sourceType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sourceType"), sourceType));
        }
        if (sourceId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sourceId"), sourceId));
        }

        if (!isAdminOrLeader(currentUser)) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.equal(root.get("ownerId"), currentUser.getUserId()),
                    cb.equal(root.get("createdBy"), currentUser.getUserId())
            ));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Rectification> rectificationPage = rectificationRepository.findAll(spec, pageable);

        RectificationListResponse response = new RectificationListResponse();
        response.setContent(rectificationPage.getContent().stream().map(RectificationListItemDTO::from).toList());
        response.setTotalElements(rectificationPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public RectificationDetailDTO detail(CurrentUser currentUser, Long id) {
        ensureRectificationModuleRole(currentUser);
        Rectification rectification = getRectificationOrThrow(id);
        assertCanAccessRectification(currentUser, rectification);
        return toDetail(rectification);
    }

    @Transactional
    public RectificationDetailDTO transition(CurrentUser currentUser, Long id, RectificationTransitionRequest request) {
        ensureRectificationModuleRole(currentUser);

        String from = request.getFrom().toLowerCase(Locale.ROOT);
        String to = request.getTo().toLowerCase(Locale.ROOT);

        validateStatus(from);
        validateStatus(to);

        Rectification rectification = getRectificationOrThrow(id);
        assertCanAccessRectification(currentUser, rectification);

        if (!isAllowedTransition(from, to)) {
            throw badRequest("非法状态迁移: " + from + " -> " + to);
        }
        if (TERMINAL_STATUS.contains(from)) {
            throw badRequest("终态不可迁移");
        }

        assertCanTransition(currentUser, rectification, from, to);

        int updated = rectificationRepository.updateStatusIfMatch(id, from, to, LocalDateTime.now());
        if (updated == 0) {
            throw badRequest("状态不匹配或已被其他人更新");
        }

        saveAction(id, "comment", currentUser.getUserId(), request.getComment(), null, null);
        Rectification latest = getRectificationOrThrow(id);
        if ("executing".equals(to)) {
            taskIntegrationService.createRectificationExecutionTask(latest, currentUser.getUserId());
        }
        return toDetail(latest);
    }

    @Transactional
    public RectificationActionDTO addAction(CurrentUser currentUser, Long id, CreateRectificationActionRequest request) {
        ensureRectificationModuleRole(currentUser);

        String actionType = request.getActionType().toLowerCase(Locale.ROOT);
        if (!VALID_ACTION.contains(actionType)) {
            throw badRequest("不支持的 actionType: " + actionType);
        }

        Rectification rectification = getRectificationOrThrow(id);
        assertCanAccessRectification(currentUser, rectification);
        assertCanAddAction(currentUser, rectification, actionType);

        RectificationAction action = saveAction(
                id,
                actionType,
                currentUser.getUserId(),
                request.getContent(),
                toJson(request.getAttachments()),
                toJson(request.getExtraJson())
        );
        return RectificationActionDTO.from(action);
    }

    @Transactional
    public RectificationCreateResponse createFromAlarm(CurrentUser currentUser, Long alarmId) {
        ensureRectificationModuleRole(currentUser);

        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new NotFoundException("报警不存在"));

        permissionService.assertCanAccessElder(currentUser, alarm.getElderId());

        boolean allowed = isAdminOrLeader(currentUser) || alarm.getAcceptedBy() != null
                && alarm.getAcceptedBy().equals(currentUser.getUserId());
        if (!allowed) {
            throw new AccessDeniedException("无权限从该报警发起整改");
        }
        assertAlarmRectificationNotDuplicated("alarm", alarmId);

        Rectification rectification = new Rectification();
        rectification.setSourceType("alarm");
        rectification.setSourceId(alarmId);
        rectification.setTitle("报警整改：" + alarm.getAlarmType() + "/" + alarm.getSeverity());
        rectification.setDescription(buildAlarmRectificationDescription(alarm));
        rectification.setLevel(mapSeverityToLevel(alarm.getSeverity()));
        rectification.setOwnerId(alarm.getAcceptedBy() == null ? currentUser.getUserId() : alarm.getAcceptedBy());
        rectification.setDueAt(LocalDateTime.now().plusDays(7));
        rectification.setStatus("open");
        rectification.setCreatedBy(currentUser.getUserId());
        rectification.setCreatedAt(LocalDateTime.now());
        rectification.setUpdatedAt(LocalDateTime.now());

        Rectification saved = rectificationRepository.save(rectification);
        saveAction(saved.getRectificationId(), "comment", currentUser.getUserId(), "由报警结案发起整改", null, null);
        return new RectificationCreateResponse(saved.getRectificationId());
    }

    private void assertAlarmRectificationNotDuplicated(String sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return;
        }
        if (!"alarm".equalsIgnoreCase(sourceType)) {
            return;
        }
        if (rectificationRepository.existsBySourceTypeAndSourceId("alarm", sourceId)) {
            throw badRequest("该报警已发起整改，不可重复发起");
        }
    }

    private Rectification getRectificationOrThrow(Long id) {
        return rectificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("整改不存在"));
    }

    private RectificationDetailDTO toDetail(Rectification rectification) {
        RectificationDetailDTO dto = new RectificationDetailDTO();
        dto.setRectificationId(rectification.getRectificationId());
        dto.setSourceType(rectification.getSourceType());
        dto.setSourceId(rectification.getSourceId());
        dto.setTitle(rectification.getTitle());
        dto.setDescription(rectification.getDescription());
        dto.setLevel(rectification.getLevel());
        dto.setOwnerId(rectification.getOwnerId());
        dto.setDueAt(rectification.getDueAt());
        dto.setStatus(rectification.getStatus());
        dto.setCreatedBy(rectification.getCreatedBy());
        dto.setProcessInstanceId(rectification.getProcessInstanceId());
        dto.setCreatedAt(rectification.getCreatedAt());
        dto.setUpdatedAt(rectification.getUpdatedAt());
        dto.setActions(rectificationActionRepository.findByRectificationIdOrderByActionTimeAsc(rectification.getRectificationId())
                .stream().map(RectificationActionDTO::from).toList());
        return dto;
    }

    private RectificationAction saveAction(Long rectificationId,
                                           String actionType,
                                           Long actorId,
                                           String content,
                                           String attachmentsJson,
                                           String extraJson) {
        RectificationAction action = new RectificationAction();
        action.setRectificationId(rectificationId);
        action.setActionType(actionType);
        action.setActorId(actorId);
        action.setActionTime(LocalDateTime.now());
        action.setContent(content);
        action.setAttachmentsJson(attachmentsJson);
        action.setExtraJson(extraJson);
        return rectificationActionRepository.save(action);
    }

    private void assertCanAccessRectification(CurrentUser user, Rectification rectification) {
        if (isAdminOrLeader(user)) {
            return;
        }
        if (rectification.getOwnerId().equals(user.getUserId()) || rectification.getCreatedBy().equals(user.getUserId())) {
            return;
        }
        throw new AccessDeniedException("无权限访问该整改记录");
    }

    private void assertCanTransition(CurrentUser user, Rectification rectification, String from, String to) {
        if (isAdminOrLeader(user)) {
            return;
        }
        if (rectification.getOwnerId().equals(user.getUserId()) && "executing".equals(from) && "verifying".equals(to)) {
            return;
        }
        throw new AccessDeniedException("无权限执行状态迁移");
    }

    private void assertCanAddAction(CurrentUser user, Rectification rectification, String actionType) {
        if (isAdminOrLeader(user)) {
            return;
        }
        if (!rectification.getOwnerId().equals(user.getUserId())) {
            throw new AccessDeniedException("无权限添加整改动作");
        }
        if ("accept".equals(actionType) || "reject".equals(actionType)) {
            throw new AccessDeniedException("仅管理员或护士长可执行验收动作");
        }

        String status = rectification.getStatus();
        if ("executing".equals(status) && ("submit".equals(actionType) || "comment".equals(actionType))) {
            return;
        }
        if ("verifying".equals(status) && ("submit".equals(actionType) || "comment".equals(actionType))) {
            return;
        }
        throw new AccessDeniedException("当前状态不允许该动作");
    }

    private boolean isAllowedTransition(String from, String to) {
        if ("open".equals(from) && "analyzing".equals(to)) {
            return true;
        }
        if ("analyzing".equals(from) && "planning".equals(to)) {
            return true;
        }
        if ("planning".equals(from) && "executing".equals(to)) {
            return true;
        }
        if ("executing".equals(from) && "verifying".equals(to)) {
            return true;
        }
        if ("verifying".equals(from) && "closed".equals(to)) {
            return true;
        }
        if ("verifying".equals(from) && "executing".equals(to)) {
            return true;
        }
        return "cancelled".equals(to) && !TERMINAL_STATUS.contains(from);
    }

    private boolean isAdminOrLeader(CurrentUser user) {
        return user.hasRole("admin") || user.hasRole("nurse_leader");
    }

    private void ensureRectificationModuleRole(CurrentUser user) {
        if (user.hasRole("family") || user.hasRole("elder")) {
            throw new AccessDeniedException("family/elder 用户无权访问整改模块");
        }
    }

    private void validateStatus(String status) {
        if (!VALID_STATUS.contains(status)) {
            throw badRequest("无效状态: " + status);
        }
    }

    private void validateLevel(String level) {
        String normalized = level == null ? "" : level.toLowerCase(Locale.ROOT);
        if (!VALID_LEVEL.contains(normalized)) {
            throw badRequest("无效等级: " + level);
        }
    }

    private String mapSeverityToLevel(String severity) {
        if (severity == null) {
            return "minor";
        }
        return switch (severity.toLowerCase(Locale.ROOT)) {
            case "critical", "high" -> "critical";
            case "warning", "medium" -> "major";
            case "info", "low" -> "minor";
            default -> "minor";
        };
    }

    private String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw badRequest("JSON 序列化失败");
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String buildAlarmRectificationDescription(Alarm alarm) {
        return "位置=" + nullSafe(alarm.getLocationText())
                + "; 创建时间=" + alarm.getCreatedAt()
                + "; 结案时间=" + alarm.getClosedAt()
                + "; 结案原因=" + nullSafe(alarm.getCloseReason());
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }
}
