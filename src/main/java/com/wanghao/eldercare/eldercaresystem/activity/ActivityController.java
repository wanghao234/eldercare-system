package com.wanghao.eldercare.eldercaresystem.activity;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/activities")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_FAMILY,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ELDER)")
public class ActivityController {

    private final ActivityService activityService;
    private final CurrentUserUtils currentUserUtils;

    public ActivityController(ActivityService activityService, CurrentUserUtils currentUserUtils) {
        this.activityService = activityService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping
    public ApiResponse<ActivityPageResponse<ActivityDTO>> listActivities(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(activityService.listActivities(from, to, page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.CREATE, entityType = "activities", responseIdPath = "activityId",
            requestFields = {"title", "activityTime", "location"})
    public ApiResponse<ActivityDTO> createActivity(@Valid @RequestBody ActivityUpsertRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(activityService.createActivity(currentUser, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.UPDATE, entityType = "activities", entityIdArg = "id",
            requestFields = {"title", "activityTime", "location"})
    public ApiResponse<ActivityDTO> updateActivity(@PathVariable Long id,
                                                   @Valid @RequestBody ActivityUpsertRequest request) {
        return ApiResponse.ok(activityService.updateActivity(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.DELETE, entityType = "activities", entityIdArg = "id")
    public ApiResponse<Void> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/signup")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_FAMILY)")
    @Audited(action = AuditAction.CREATE, entityType = "activity_participants", responseIdPath = "id",
            requestFields = {"elderId"})
    public ApiResponse<ActivityParticipantDTO> signup(@PathVariable Long id,
                                                      @Valid @RequestBody ActivityElderActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(activityService.signup(currentUser, id, request.getElderId()));
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    @Audited(action = AuditAction.TRANSITION, entityType = "activity_participants", entityIdArg = "id",
            fromValue = "signed", toValue = "checked_in", requestFields = {"elderId"})
    public ApiResponse<ActivityParticipantDTO> checkIn(@PathVariable Long id,
                                                       @Valid @RequestBody ActivityElderActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(activityService.checkIn(currentUser, id, request.getElderId()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_FAMILY)")
    @Audited(action = AuditAction.TRANSITION, entityType = "activity_participants", entityIdArg = "id",
            fromValue = "signed|checked_in", toValue = "cancelled", requestFields = {"elderId"})
    public ApiResponse<ActivityParticipantDTO> cancel(@PathVariable Long id,
                                                      @Valid @RequestBody ActivityElderActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(activityService.cancel(currentUser, id, request.getElderId()));
    }

    @GetMapping("/{id}/participants")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
    public ApiResponse<ActivityPageResponse<ActivityParticipantDTO>> listParticipants(@PathVariable Long id,
                                                                                       @RequestParam(defaultValue = "0") int page,
                                                                                       @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(activityService.listParticipants(id, page, size));
    }
}
