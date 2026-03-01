package com.wanghao.eldercare.eldercaresystem.visit;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.security.scope.BizScoped;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitService visitService;
    private final CurrentUserUtils currentUserUtils;

    public VisitController(VisitService visitService, CurrentUserUtils currentUserUtils) {
        this.visitService = visitService;
        this.currentUserUtils = currentUserUtils;
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, entityType = "visit_requests", responseIdPath = "requestId")
    @PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_FAMILY)")
    public ApiResponse<VisitCreateResponse> create(@Valid @RequestBody CreateVisitRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(visitService.create(currentUser, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_FAMILY)")
    public ApiResponse<VisitListResponse> list(@RequestParam(required = false) Long elderId,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String requestType,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(visitService.list(currentUser, elderId, status, requestType, from, to, page, size));
    }

    @GetMapping("/{id}")
    @BizScoped(type = "visit", idParam = "id")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_FAMILY)")
    public ApiResponse<VisitDetailDTO> detail(@PathVariable Long id) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(visitService.detail(currentUser, id));
    }

    @PostMapping("/{id}/confirm")
    @Audited(action = AuditAction.TRANSITION, entityType = "visit_requests", entityIdArg = "id", fromValue = "pending", toValue = "confirmed")
    @BizScoped(type = "visit", idParam = "id")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<VisitDetailDTO> confirm(@PathVariable Long id,
                                               @RequestBody(required = false) VisitActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(visitService.confirm(currentUser, id, request));
    }

    @PostMapping("/{id}/approve")
    @Audited(action = AuditAction.TRANSITION, entityType = "visit_requests", entityIdArg = "id", fromValue = "confirmed", toValue = "approved")
    @BizScoped(type = "visit", idParam = "id")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
    public ApiResponse<VisitDetailDTO> approve(@PathVariable Long id,
                                               @RequestBody(required = false) VisitActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(visitService.approve(currentUser, id, request));
    }

    @PostMapping("/{id}/reject")
    @Audited(action = AuditAction.TRANSITION, entityType = "visit_requests", entityIdArg = "id", fromValue = "pending|confirmed", toValue = "rejected")
    @BizScoped(type = "visit", idParam = "id")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
    public ApiResponse<VisitDetailDTO> reject(@PathVariable Long id,
                                              @RequestBody(required = false) VisitActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(visitService.reject(currentUser, id, request));
    }

    @PostMapping("/{id}/check-out")
    @Audited(action = AuditAction.TRANSITION, entityType = "visit_requests", entityIdArg = "id", fromValue = "approved", toValue = "in_progress")
    @BizScoped(type = "visit", idParam = "id")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<VisitDetailDTO> checkOut(@PathVariable Long id,
                                                @RequestBody(required = false) VisitActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(visitService.checkOut(currentUser, id, request));
    }

    @PostMapping("/{id}/check-in")
    @Audited(action = AuditAction.TRANSITION, entityType = "visit_requests", entityIdArg = "id", fromValue = "in_progress", toValue = "completed")
    @BizScoped(type = "visit", idParam = "id")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<VisitDetailDTO> checkIn(@PathVariable Long id,
                                               @RequestBody(required = false) VisitActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(visitService.checkIn(currentUser, id, request));
    }

    @PostMapping("/{id}/cancel")
    @Audited(action = AuditAction.TRANSITION, entityType = "visit_requests", entityIdArg = "id", fromValue = "pending|confirmed|approved", toValue = "cancelled")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_FAMILY)")
    public ApiResponse<VisitDetailDTO> cancel(@PathVariable Long id,
                                              @RequestBody(required = false) VisitActionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(visitService.cancel(currentUser, id, request));
    }
}
