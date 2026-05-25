package com.wanghao.eldercare.eldercaresystem.controller.activity;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.activity.*;
import com.wanghao.eldercare.eldercaresystem.entity.activity.*;
import com.wanghao.eldercare.eldercaresystem.mapper.activity.*;
import com.wanghao.eldercare.eldercaresystem.service.activity.*;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activities")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_FAMILY,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ELDER)")
public class ActivityController {

    private final ActivityService activityService;
    private final CurrentUserUtils currentUserUtils;

    public ActivityController(ActivityService activityService, CurrentUserUtils currentUserUtils) {
        this.activityService = activityService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping
    public ApiResponse<ActivityPageResponse<ActivityDTO>> listActivities(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(activityService.listActivities(parseDateTimeParam(from, "from"),
                parseDateTimeParam(to, "to"), page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.CREATE, entityType = "activities", responseIdPath = "activityId",
            requestFields = {"title", "activityTime", "location"})
    public ApiResponse<ActivityDTO> createActivity(@Valid @RequestBody ActivityUpsertRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(activityService.createActivity(currentUser, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.UPDATE, entityType = "activities", entityIdArg = "id",
            requestFields = {"title", "activityTime", "location"})
    public ApiResponse<ActivityDTO> updateActivity(@PathVariable Long id,
                                                   @Valid @RequestBody ActivityUpsertRequest request) {
        return ApiResponse.ok(activityService.updateActivity(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.DELETE, entityType = "activities", entityIdArg = "id")
    public ApiResponse<Void> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/signup")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_FAMILY)")
    @Audited(action = AuditAction.CREATE, entityType = "activity_participants", responseIdPath = "id",
            requestFields = {"elderId"})
    public ApiResponse<ActivityParticipantDTO> signup(@PathVariable Long id,
                                                      @Valid @RequestBody ActivityElderActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(activityService.signup(currentUser, id, request.getElderId()));
    }

    @PostMapping("/{id}/signup-batch")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_FAMILY,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ELDER)")
    @Audited(action = AuditAction.CREATE, entityType = "activity_participants", requestFields = {"elderIds"})
    public ApiResponse<ActivityBatchSignupResponse> signupBatch(@PathVariable Long id,
                                                                @RequestBody(required = false) ActivityBatchSignupRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(activityService.signupBatch(currentUser, id, request == null ? null : request.getElderIds()));
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    @Audited(action = AuditAction.TRANSITION, entityType = "activity_participants", entityIdArg = "id",
            fromValue = "signed", toValue = "checked_in", requestFields = {"elderId"})
    public ApiResponse<ActivityParticipantDTO> checkIn(@PathVariable Long id,
                                                       @Valid @RequestBody ActivityElderActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(activityService.checkIn(currentUser, id, request.getElderId()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_FAMILY)")
    @Audited(action = AuditAction.TRANSITION, entityType = "activity_participants", entityIdArg = "id",
            fromValue = "signed|checked_in", toValue = "cancelled", requestFields = {"elderId"})
    public ApiResponse<ActivityParticipantDTO> cancel(@PathVariable Long id,
                                                      @Valid @RequestBody ActivityElderActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(activityService.cancel(currentUser, id, request.getElderId()));
    }

    @GetMapping("/{id}/participants")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    public ApiResponse<ActivityPageResponse<ActivityParticipantDTO>> listParticipants(@PathVariable Long id,
                                                                                       @RequestParam(defaultValue = "0") int page,
                                                                                       @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(activityService.listParticipants(id, page, size));
    }

    @GetMapping("/{id}/participant-stats")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    public ApiResponse<ActivityParticipantStatsResponse> participantStats(@PathVariable Long id) {
        return ApiResponse.ok(activityService.participantStats(id));
    }

    private LocalDateTime parseDateTimeParam(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ignore) {
            // fallback below
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT));
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    fieldName + " 时间格式错误，支持 yyyy-MM-dd HH:mm:ss 或 ISO-8601（如 2026-05-01T00:00:00）",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
