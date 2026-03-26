package com.wanghao.eldercare.eldercaresystem.controller.qc;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.RequirePerm;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.qc.*;
import com.wanghao.eldercare.eldercaresystem.entity.qc.*;
import com.wanghao.eldercare.eldercaresystem.mapper.qc.*;
import com.wanghao.eldercare.eldercaresystem.service.qc.*;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/qc")
@RequirePerm("qc:manage")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
public class QcController {

    private final QcService qcService;
    private final CurrentUserUtils currentUserUtils;

    public QcController(QcService qcService, CurrentUserUtils currentUserUtils) {
        this.qcService = qcService;
        this.currentUserUtils = currentUserUtils;
    }

    @PostMapping("/audits")
    public ApiResponse<IdResponse> createAudit(@RequestBody(required = false) CreateQcAuditRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        CreateQcAuditRequest body = request == null ? new CreateQcAuditRequest() : request;
        return ApiResponse.ok(qcService.createAudit(currentUser, body));
    }

    @GetMapping("/audits")
    public ApiResponse<QcAuditListResponse> listAudits(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(qcService.listAudits(currentUser, page, size));
    }

    @GetMapping("/audits/{id}")
    public ApiResponse<QcAuditDTO> getAudit(@PathVariable Long id) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(qcService.getAuditDetail(currentUser, id));
    }

    @PostMapping("/audits/{auditId}/items/{itemId}/check")
    public ApiResponse<QcAuditItemDTO> checkItem(@PathVariable Long auditId,
                                                 @PathVariable Long itemId,
                                                 @Valid @RequestBody CheckQcItemRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(qcService.checkItem(currentUser, auditId, itemId, request));
    }

    @PostMapping("/issues")
    public ApiResponse<QcIssueDTO> createIssue(@Valid @RequestBody CreateQcIssueRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(qcService.createIssue(currentUser, request));
    }

    @GetMapping("/issues")
    public ApiResponse<List<QcIssueDTO>> listIssues(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(qcService.listIssues(currentUser, page, size));
    }
}
