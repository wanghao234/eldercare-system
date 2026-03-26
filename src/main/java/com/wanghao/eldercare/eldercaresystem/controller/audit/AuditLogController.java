package com.wanghao.eldercare.eldercaresystem.controller.audit;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtil;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.RequirePerm;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.audit.*;
import com.wanghao.eldercare.eldercaresystem.dto.audit.AuditLogDTO;
import com.wanghao.eldercare.eldercaresystem.dto.audit.AuditLogQuery;
import com.wanghao.eldercare.eldercaresystem.dto.audit.PageResponse;
import com.wanghao.eldercare.eldercaresystem.entity.audit.*;
import com.wanghao.eldercare.eldercaresystem.mapper.audit.*;
import com.wanghao.eldercare.eldercaresystem.service.audit.*;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_DOCTOR)")
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
