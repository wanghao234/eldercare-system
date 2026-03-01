package com.wanghao.eldercare.eldercaresystem.alarm;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.security.RoleMapper;
import com.wanghao.eldercare.eldercaresystem.task.TaskIntegrationService;
import com.wanghao.eldercare.eldercaresystem.workflow.WorkflowService;
import com.wanghao.eldercare.eldercaresystem.ws.AlarmMessagePublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
@Service
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final AlarmActionLogRepository alarmActionLogRepository;
    private final PermissionService permissionService;
    private final AlarmMessagePublisher alarmMessagePublisher;
    private final WorkflowService workflowService;
    private final TaskIntegrationService taskIntegrationService;

    public AlarmService(AlarmRepository alarmRepository,
                        AlarmActionLogRepository alarmActionLogRepository,
                        PermissionService permissionService,
                        AlarmMessagePublisher alarmMessagePublisher,
                        WorkflowService workflowService,
                        TaskIntegrationService taskIntegrationService) {
        this.alarmRepository = alarmRepository;
        this.alarmActionLogRepository = alarmActionLogRepository;
        this.permissionService = permissionService;
        this.alarmMessagePublisher = alarmMessagePublisher;
        this.workflowService = workflowService;
        this.taskIntegrationService = taskIntegrationService;
    }

    @Transactional
    public AlarmCreateResponse createAlarm(CurrentUser currentUser, CreateAlarmRequest request) {
        ensureCanCreateAlarm(currentUser);
        permissionService.assertCanAccessElder(currentUser, request.getElderId());

        Alarm alarm = new Alarm();
        alarm.setElderId(request.getElderId());
        alarm.setRoomId(request.getRoomId());
        alarm.setBedId(request.getBedId());
        alarm.setAlarmType(request.getAlarmType());
        alarm.setSeverity(request.getSeverity());
        alarm.setSource(request.getSource());
        alarm.setLocationText(request.getLocationText());
        alarm.setStatus("created");
        alarm.setCreatedAt(LocalDateTime.now());

        Alarm saved = alarmRepository.save(alarm);
        Long processInstanceId = workflowService.startAlarmWorkflow(saved.getAlarmId(), currentUser.getUserId());
        saved.setProcessInstanceId(processInstanceId);
        saved = alarmRepository.save(saved);
        writeActionLog(saved.getAlarmId(), "create", currentUser.getUserId(), request.getNote(), request.getAttachmentsJson());
        alarmMessagePublisher.publishCreated(saved);
        return new AlarmCreateResponse(saved.getAlarmId());
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
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
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
        permissionService.assertCanAccessElder(currentUser, alarm.getElderId());
        return toDetailDTO(alarm);
    }

    @Transactional
    public AlarmDetailDTO acceptAlarm(CurrentUser currentUser, Long alarmId, AlarmActionRequest request) {
        Alarm alarm = getAlarmOrThrow(alarmId);
        permissionService.assertCanAccessElder(currentUser, alarm.getElderId());

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
        permissionService.assertCanAccessElder(currentUser, alarm.getElderId());

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
        permissionService.assertCanAccessElder(currentUser, alarm.getElderId());

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
}
