package com.wanghao.eldercare.eldercaresystem.controller.alarm;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.RequirePerm;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.BizScoped;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.alarm.*;
import com.wanghao.eldercare.eldercaresystem.dto.rectification.RectificationCreateResponse;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.*;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.*;
import com.wanghao.eldercare.eldercaresystem.service.alarm.*;
import com.wanghao.eldercare.eldercaresystem.service.rectification.RectificationService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alarms")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_DOCTOR)")
public class AlarmController {

    private final AlarmService alarmService;
    private final RectificationService rectificationService;
    private final CurrentUserUtils currentUserUtils;

    public AlarmController(AlarmService alarmService,
                           RectificationService rectificationService,
                           CurrentUserUtils currentUserUtils) {
        this.alarmService = alarmService;
        this.rectificationService = rectificationService;
        this.currentUserUtils = currentUserUtils;
    }

    @PostMapping
    @PreAuthorize("permitAll()")
    @Audited(action = AuditAction.CREATE, entityType = "alarms", responseIdPath = "alarmId", requestFields = {"elderId", "alarmType", "severity", "source", "locationText"})
    public ApiResponse<AlarmCreateResponse> create(@Valid @RequestBody CreateAlarmRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUserOrSystem();
        return ApiResponse.success(alarmService.createAlarm(currentUser, request));
    }

    @GetMapping
    @RequirePerm("alarm:read")
    public ApiResponse<AlarmListResponse> list(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) String severity,
                                               @RequestParam(required = false) String alarmType,
                                               @RequestParam(required = false) Long elderId,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success(alarmService.listAlarms(currentUser, status, severity, alarmType, elderId, from, to, page, size));
    }

    @GetMapping("/{alarmId}")
    @RequirePerm("alarm:read")
    @BizScoped(type = "alarm", idParam = "alarmId")
    public ApiResponse<AlarmDetailDTO> detail(@PathVariable Long alarmId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success(alarmService.getAlarmDetail(currentUser, alarmId));
    }

    @PostMapping("/{alarmId}/accept")
    @RequirePerm("alarm:handle")
    @Audited(action = AuditAction.TRANSITION, entityType = "alarms", entityIdArg = "alarmId", fromValue = "created", toValue = "accepted")
    @BizScoped(type = "alarm", idParam = "alarmId")
    public ApiResponse<AlarmDetailDTO> accept(@PathVariable Long alarmId,
                                              @RequestBody(required = false) AlarmActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success(alarmService.acceptAlarm(currentUser, alarmId, request));
    }

    @PostMapping("/{alarmId}/arrive")
    @RequirePerm("alarm:handle")
    @Audited(action = AuditAction.TRANSITION, entityType = "alarms", entityIdArg = "alarmId", fromValue = "accepted", toValue = "on_site")
    @BizScoped(type = "alarm", idParam = "alarmId")
    public ApiResponse<AlarmDetailDTO> arrive(@PathVariable Long alarmId,
                                              @RequestBody(required = false) AlarmActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success(alarmService.arriveAlarm(currentUser, alarmId, request));
    }

    @PostMapping("/{alarmId}/close")
    @RequirePerm("alarm:handle")
    @Audited(action = AuditAction.TRANSITION, entityType = "alarms", entityIdArg = "alarmId", fromValue = "on_site|handling", toValue = "closed", requestFields = {"closeReason", "note"})
    @BizScoped(type = "alarm", idParam = "alarmId")
    public ApiResponse<AlarmDetailDTO> close(@PathVariable Long alarmId,
                                             @Valid @RequestBody CloseAlarmRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success(alarmService.closeAlarm(currentUser, alarmId, request));
    }

    @PostMapping("/{alarmId}/bind-elder")
    @RequirePerm("alarm:handle")
    @Audited(action = AuditAction.UPDATE, entityType = "alarms", entityIdArg = "alarmId", requestFields = {"elderId", "note"})
    @BizScoped(type = "alarm", idParam = "alarmId")
    public ApiResponse<AlarmDetailDTO> bindElder(@PathVariable Long alarmId,
                                                 @Valid @RequestBody BindAlarmElderRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success(alarmService.bindAlarmElder(currentUser, alarmId, request));
    }

    @PostMapping("/{alarmId}/rectifications")
    @RequirePerm("alarm:handle")
    @Audited(action = AuditAction.CREATE, entityType = "rectifications", entityIdArg = "alarmId", responseIdPath = "rectificationId")
    @BizScoped(type = "alarm", idParam = "alarmId")
    public ApiResponse<RectificationCreateResponse> createRectification(@PathVariable Long alarmId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(rectificationService.createFromAlarm(currentUser, alarmId));
    }
}
