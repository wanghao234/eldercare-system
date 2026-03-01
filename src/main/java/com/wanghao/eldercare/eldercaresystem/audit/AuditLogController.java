package com.wanghao.eldercare.eldercaresystem.audit;

import com.wanghao.eldercare.eldercaresystem.audit.dto.AuditLogDTO;
import com.wanghao.eldercare.eldercaresystem.audit.dto.AuditLogQuery;
import com.wanghao.eldercare.eldercaresystem.audit.dto.PageResponse;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtil;
import com.wanghao.eldercare.eldercaresystem.security.perm.RequirePerm;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;
    private final CurrentUserUtil currentUserUtil;

    public AuditLogController(AuditLogQueryService auditLogQueryService, CurrentUserUtil currentUserUtil) {
        this.auditLogQueryService = auditLogQueryService;
        this.currentUserUtil = currentUserUtil;
    }

    @GetMapping
    @RequirePerm("audit:read")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_DOCTOR)")
    public ApiResponse<PageResponse<AuditLogDTO>> query(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(required = false) Long userId,
                                                        @RequestParam(required = false) String action,
                                                        @RequestParam(required = false) String entityType,
                                                        @RequestParam(required = false) Long entityId,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        CurrentUser currentUser = currentUserUtil.getCurrentUser();
        AuditLogQuery query = new AuditLogQuery();
        query.setUserId(userId);
        query.setAction(action);
        query.setEntityType(entityType);
        query.setEntityId(entityId);
        query.setFrom(from);
        query.setTo(to);
        return ApiResponse.ok(auditLogQueryService.query(currentUser, query, page, size));
    }
}
