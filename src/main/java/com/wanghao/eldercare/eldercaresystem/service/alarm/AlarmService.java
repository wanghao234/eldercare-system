package com.wanghao.eldercare.eldercaresystem.service.alarm;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.common.security.RoleMapper;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.AlarmMessagePublisher;
import com.wanghao.eldercare.eldercaresystem.controller.alarm.*;
import com.wanghao.eldercare.eldercaresystem.dto.alarm.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.*;
import com.wanghao.eldercare.eldercaresystem.entity.admission.Bed;
import com.wanghao.eldercare.eldercaresystem.entity.facility.CameraDevice;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.BedRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.RoomRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.*;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.CameraDeviceRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.task.TaskIntegrationService;
import com.wanghao.eldercare.eldercaresystem.service.workflow.WorkflowService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final AlarmActionLogRepository alarmActionLogRepository;
    private final PermissionService permissionService;
    private final AlarmMessagePublisher alarmMessagePublisher;
    private final WorkflowService workflowService;
    private final TaskIntegrationService taskIntegrationService;
    private final CameraDeviceRepository cameraDeviceRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final UserRepository userRepository;

    public AlarmService(AlarmRepository alarmRepository,
                        AlarmActionLogRepository alarmActionLogRepository,
                        PermissionService permissionService,
                        AlarmMessagePublisher alarmMessagePublisher,
                        WorkflowService workflowService,
                        TaskIntegrationService taskIntegrationService,
                        CameraDeviceRepository cameraDeviceRepository,
                        RoomRepository roomRepository,
                        BedRepository bedRepository,
                        UserRepository userRepository) {
        this.alarmRepository = alarmRepository;
        this.alarmActionLogRepository = alarmActionLogRepository;
        this.permissionService = permissionService;
        this.alarmMessagePublisher = alarmMessagePublisher;
        this.workflowService = workflowService;
        this.taskIntegrationService = taskIntegrationService;
        this.cameraDeviceRepository = cameraDeviceRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AlarmCreateResponse createAlarm(CurrentUser currentUser, CreateAlarmRequest request) {
        ensureCanCreateAlarm(currentUser);

        String idempotencyKey = trimToNull(request.getIdempotencyKey());
        if (idempotencyKey != null) {
            Optional<Alarm> existing = alarmRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return new AlarmCreateResponse(existing.get().getAlarmId());
            }
        }

        CameraDevice cameraDevice = null;
        if (request.getCameraId() != null) {
            cameraDevice = cameraDeviceRepository.findById(request.getCameraId())
                    .orElseThrow(() -> new NotFoundException("摄像头不存在"));
        }

        Long elderId = request.getElderId();
        boolean elderIdFromCamera = false;
        if (elderId == null && cameraDevice != null) {
            elderId = cameraDevice.getElderId();
            elderIdFromCamera = elderId != null;
        }
        if (elderId != null) {
            if (elderIdFromCamera) {
                elderId = resolveCameraBoundElderId(elderId);
            } else {
                ensureElderExists(elderId);
            }
        }
        if (elderId != null && !currentUser.hasRole("system")) {
            permissionService.assertCanAccessElder(currentUser, elderId);
        }

        Long rawRoomId = firstNonNull(request.getRoomId(), cameraDevice == null ? null : cameraDevice.getRoomId());
        Long rawBedId = firstNonNull(request.getBedId(), cameraDevice == null ? null : cameraDevice.getBedId());
        RoomBedRef normalizedRef = normalizeRoomBedRef(rawRoomId, rawBedId);

        Alarm alarm = new Alarm();
        alarm.setElderId(elderId);
        alarm.setRoomId(normalizedRef.roomId());
        alarm.setBedId(normalizedRef.bedId());
        alarm.setAlarmType(defaultIfBlank(request.getAlarmType(), "fall"));
        alarm.setSeverity(defaultIfBlank(request.getSeverity(), "high"));
        alarm.setSource(defaultIfBlank(request.getSource(), "manual"));
        alarm.setLocationText(firstNonBlank(request.getLocationText(), cameraDevice == null ? null : cameraDevice.getLocationText()));
        alarm.setStatus("created");
        alarm.setCreatedAt(LocalDateTime.now());
        alarm.setCameraId(request.getCameraId());
        alarm.setConfidence(request.getConfidence());
        alarm.setSnapshotUrl(trimToNull(request.getSnapshotUrl()));
        alarm.setAttachmentsJson(trimToNull(request.getAttachmentsJson()));
        alarm.setMapX(firstNonNull(request.getMapX(), cameraDevice == null ? null : cameraDevice.getMapX()));
        alarm.setMapY(firstNonNull(request.getMapY(), cameraDevice == null ? null : cameraDevice.getMapY()));
        alarm.setIdempotencyKey(idempotencyKey);

        Alarm saved = alarmRepository.save(alarm);
        Long processInstanceId = workflowService.startAlarmWorkflow(saved.getAlarmId(), currentUser.getUserId());
        saved.setProcessInstanceId(processInstanceId);
        saved = alarmRepository.save(saved);
        writeActionLog(saved.getAlarmId(), "create", currentUser.getUserId(), request.getNote(), request.getAttachmentsJson());
        alarmMessagePublisher.publishCreated(saved);
        return new AlarmCreateResponse(saved.getAlarmId());
    }

    @Transactional
    public AlarmDetailDTO bindAlarmElder(CurrentUser currentUser, Long alarmId, BindAlarmElderRequest request) {
        Alarm alarm = getAlarmOrThrow(alarmId);
        if (!isAdminOrLeader(currentUser)) {
            throw new AccessDeniedException("仅管理员或护士长可绑定老人");
        }
        if (request == null || request.getElderId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "elderId 不能为空", HttpStatus.BAD_REQUEST);
        }

        ensureElderExists(request.getElderId());
        alarm.setElderId(request.getElderId());
        Alarm saved = alarmRepository.save(alarm);
        writeActionLog(alarmId, "bind_elder", currentUser.getUserId(), request.getNote(), null);
        workflowService.appendAlarmAction(alarmId, currentUser.getUserId(), "bind_elder",
                "报警补充绑定老人，elderId=" + request.getElderId());
        alarmMessagePublisher.publishUpdated(saved, currentUser.getUsername());
        return toDetailDTO(saved);
    }

    @Transactional(readOnly = true)
    public AlarmListResponse listAlarms(CurrentUser currentUser,
                                        String status,
                                        String severity,
                                        String alarmType,
                                        Long elderId,
                                        LocalDateTime from,
                                        LocalDateTime to,
                                        int page,
                                        int size) {
        List<Long> visibleElderIds = canViewAllAlarms(currentUser) ? null : permissionService.getVisibleElderIds(currentUser);
        if (visibleElderIds != null && visibleElderIds.isEmpty()) {
            AlarmListResponse empty = new AlarmListResponse();
            empty.setContent(Collections.emptyList());
            empty.setTotalElements(0);
            empty.setPage(page);
            empty.setSize(size);
            return empty;
        }

        Specification<Alarm> spec = Specification.where(null);

        if (visibleElderIds != null) {
            spec = spec.and((root, query, cb) -> root.get("elderId").in(visibleElderIds));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (severity != null && !severity.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        }
        if (alarmType != null && !alarmType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("alarmType"), alarmType));
        }
        if (elderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("elderId"), elderId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Alarm> alarmPage = alarmRepository.findAll(spec, pageable);

        AlarmListResponse response = new AlarmListResponse();
        response.setContent(alarmPage.getContent().stream().map(AlarmListItemDTO::from).toList());
        response.setTotalElements(alarmPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public AlarmDetailDTO getAlarmDetail(CurrentUser currentUser, Long alarmId) {
        Alarm alarm = getAlarmOrThrow(alarmId);
        assertCanAccessAlarm(currentUser, alarm);
        return toDetailDTO(alarm);
    }

    @Transactional
    public AlarmDetailDTO acceptAlarm(CurrentUser currentUser, Long alarmId, AlarmActionRequest request) {
        Alarm alarm = getAlarmOrThrow(alarmId);
        assertCanAccessAlarm(currentUser, alarm);

        int updated = alarmRepository.acceptIfCreated(alarmId, LocalDateTime.now(), currentUser.getUserId());
        if (updated == 0) {
            String status = alarmRepository.findStatusByAlarmId(alarmId).orElse("unknown");
            throwStateTransitionError("accept", status);
        }

        writeActionLog(alarmId, "accept", currentUser.getUserId(), request == null ? null : request.getNote(),
                request == null ? null : request.getAttachmentsJson());
        Alarm updatedAlarm = getAlarmOrThrow(alarmId);
        workflowService.appendAlarmAction(alarmId, currentUser.getUserId(), "accept",
                "报警接单，状态变更为 accepted");
        taskIntegrationService.createAlarmActionTask(updatedAlarm, currentUser.getUserId());
        alarmMessagePublisher.publishUpdated(updatedAlarm, currentUser.getUsername());
        return toDetailDTO(updatedAlarm);
    }

    @Transactional
    public AlarmDetailDTO arriveAlarm(CurrentUser currentUser, Long alarmId, AlarmActionRequest request) {
        Alarm alarm = getAlarmOrThrow(alarmId);
        assertCanAccessAlarm(currentUser, alarm);

        int updated = alarmRepository.arriveIfAccepted(alarmId, LocalDateTime.now(), currentUser.getUserId());
        if (updated == 0) {
            String status = alarmRepository.findStatusByAlarmId(alarmId).orElse("unknown");
            throwStateTransitionError("arrive", status);
        }

        writeActionLog(alarmId, "arrive", currentUser.getUserId(), request == null ? null : request.getNote(),
                request == null ? null : request.getAttachmentsJson());
        Alarm updatedAlarm = getAlarmOrThrow(alarmId);
        workflowService.appendAlarmAction(alarmId, currentUser.getUserId(), "arrive",
                "报警到场，状态变更为 on_site");
        alarmMessagePublisher.publishUpdated(updatedAlarm, currentUser.getUsername());
        return toDetailDTO(updatedAlarm);
    }

    @Transactional
    public AlarmDetailDTO closeAlarm(CurrentUser currentUser, Long alarmId, CloseAlarmRequest request) {
        Alarm alarm = getAlarmOrThrow(alarmId);
        assertCanAccessAlarm(currentUser, alarm);

        LocalDateTime now = LocalDateTime.now();
        int updated = alarmRepository.closeIfOnSite(alarmId, now, currentUser.getUserId(), request.getCloseReason());
        if (updated == 0) {
            updated = alarmRepository.closeIfHandling(alarmId, now, currentUser.getUserId(), request.getCloseReason());
        }
        if (updated == 0) {
            String status = alarmRepository.findStatusByAlarmId(alarmId).orElse("unknown");
            throwStateTransitionError("close", status);
        }

        writeActionLog(alarmId, "close", currentUser.getUserId(), request.getNote(), request.getAttachmentsJson());
        Alarm updatedAlarm = getAlarmOrThrow(alarmId);
        workflowService.appendAlarmAction(alarmId, currentUser.getUserId(), "close",
                "报警结案，状态变更为 closed");
        alarmMessagePublisher.publishUpdated(updatedAlarm, currentUser.getUsername());
        return toDetailDTO(updatedAlarm);
    }

    private Alarm getAlarmOrThrow(Long alarmId) {
        return alarmRepository.findById(alarmId)
                .orElseThrow(() -> new NotFoundException("报警不存在"));
    }

    private AlarmDetailDTO toDetailDTO(Alarm alarm) {
        AlarmDetailDTO dto = new AlarmDetailDTO();
        dto.setAlarmId(alarm.getAlarmId());
        dto.setElderId(alarm.getElderId());
        dto.setRoomId(alarm.getRoomId());
        dto.setBedId(alarm.getBedId());
        dto.setAlarmType(alarm.getAlarmType());
        dto.setSeverity(alarm.getSeverity());
        dto.setSource(alarm.getSource());
        dto.setLocationText(alarm.getLocationText());
        dto.setStatus(alarm.getStatus());
        dto.setCreatedAt(alarm.getCreatedAt());
        dto.setAcceptedAt(alarm.getAcceptedAt());
        dto.setAcceptedBy(alarm.getAcceptedBy());
        dto.setArrivedAt(alarm.getArrivedAt());
        dto.setArrivedBy(alarm.getArrivedBy());
        dto.setClosedAt(alarm.getClosedAt());
        dto.setClosedBy(alarm.getClosedBy());
        dto.setCloseReason(alarm.getCloseReason());
        dto.setProcessInstanceId(alarm.getProcessInstanceId());

        List<AlarmActionLogDTO> logs = alarmActionLogRepository.findByAlarmIdOrderByActionTimeAsc(alarm.getAlarmId())
                .stream().map(AlarmActionLogDTO::from).toList();
        dto.setActionLogs(logs);
        return dto;
    }

    private void writeActionLog(Long alarmId, String action, Long actorId, String note, String attachmentsJson) {
        AlarmActionLog log = new AlarmActionLog();
        log.setAlarmId(alarmId);
        log.setAction(action);
        log.setActorId(actorId);
        log.setActionTime(LocalDateTime.now());
        log.setNote(note);
        log.setAttachmentsJson(attachmentsJson);
        alarmActionLogRepository.save(log);
    }

    private void ensureCanCreateAlarm(CurrentUser currentUser) {
        String role = RoleMapper.normalizeRole(currentUser.getRole());
        if ("admin".equals(role) || "nurse".equals(role) || "nurse_leader".equals(role)
                || "caregiver".equals(role) || "system".equals(role)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色不允许创建报警", HttpStatus.FORBIDDEN);
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

    private void ensureElderExists(Long elderId) {
        User elder = userRepository.findByUserIdAndDeletedAtIsNull(elderId)
                .orElseThrow(() -> new NotFoundException("老人不存在"));
        if (!"elder".equalsIgnoreCase(elder.getRole())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "绑定对象不是老人账号", HttpStatus.BAD_REQUEST);
        }
        if (!"active".equalsIgnoreCase(elder.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "老人账号未启用", HttpStatus.BAD_REQUEST);
        }
    }

    private Long resolveCameraBoundElderId(Long elderId) {
        return userRepository.findByUserIdAndDeletedAtIsNull(elderId)
                .filter(user -> "elder".equalsIgnoreCase(user.getRole()))
                .filter(user -> "active".equalsIgnoreCase(user.getStatus()))
                .map(User::getUserId)
                .orElse(null);
    }

    private boolean isAdminOrLeader(CurrentUser currentUser) {
        return currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader");
    }

    private boolean canViewAllAlarms(CurrentUser currentUser) {
        return currentUser.hasRole("admin")
                || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("nurse")
                || currentUser.hasRole("caregiver")
                || currentUser.hasRole("doctor");
    }

    private void throwStateTransitionError(String action, String currentStatus) {
        String message;
        if ("accept".equals(action)) {
            message = "接单失败：已被接单或状态不匹配，当前状态=" + currentStatus;
        } else if ("arrive".equals(action)) {
            message = "到场失败：仅允许 accepted -> on_site，当前状态=" + currentStatus;
        } else {
            message = "关闭失败：仅允许 on_site/handling -> closed，当前状态=" + currentStatus;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? trimToNull(fallback) : preferred;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    private RoomBedRef normalizeRoomBedRef(Long roomId, Long bedId) {
        Long normalizedRoomId = roomId;
        Long normalizedBedId = bedId;

        if (normalizedBedId != null) {
            Optional<Bed> bedOpt = bedRepository.findById(normalizedBedId);
            if (bedOpt.isPresent()) {
                Bed bed = bedOpt.get();
                if (normalizedRoomId == null || !roomRepository.existsById(normalizedRoomId)
                        || !normalizedRoomId.equals(bed.getRoomId())) {
                    normalizedRoomId = bed.getRoomId();
                }
            } else {
                normalizedBedId = null;
            }
        }

        if (normalizedRoomId != null && !roomRepository.existsById(normalizedRoomId)) {
            normalizedRoomId = null;
        }

        if (normalizedBedId != null && normalizedRoomId == null) {
            normalizedBedId = null;
        }

        return new RoomBedRef(normalizedRoomId, normalizedBedId);
    }

    private record RoomBedRef(Long roomId, Long bedId) {
    }
}
