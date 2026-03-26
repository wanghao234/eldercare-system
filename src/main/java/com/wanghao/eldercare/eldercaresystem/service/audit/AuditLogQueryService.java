package com.wanghao.eldercare.eldercaresystem.service.audit;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.RoleMapper;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.audit.*;
import com.wanghao.eldercare.eldercaresystem.dto.audit.*;
import com.wanghao.eldercare.eldercaresystem.dto.audit.AuditLogDTO;
import com.wanghao.eldercare.eldercaresystem.dto.audit.AuditLogQuery;
import com.wanghao.eldercare.eldercaresystem.dto.audit.PageResponse;
import com.wanghao.eldercare.eldercaresystem.entity.audit.*;
import com.wanghao.eldercare.eldercaresystem.mapper.audit.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogQueryService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> query(CurrentUser currentUser, AuditLogQuery query, int page, int size) {
        String role = normalizeRole(currentUser.getRole());
        boolean privileged = isPrivileged(role);
        if ("family".equals(role) || "elder".equals(role)) {
            throw new AccessDeniedException("当前角色无权查看审计日志");
        }

        Long enforcedUserId = query.getUserId();
        if (!privileged) {
            enforcedUserId = currentUser.getUserId();
        }
        final Long finalUserId = enforcedUserId;

        Specification<AuditLog> spec = Specification.where(null);
        if (finalUserId != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("userId"), finalUserId));
        }
        if (hasText(query.getAction())) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("action"), query.getAction()));
        }
        if (hasText(query.getEntityType())) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("entityType"), query.getEntityType()));
        }
        if (query.getEntityId() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("entityId"), query.getEntityId()));
        }
        if (query.getFrom() != null) {
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), query.getFrom()));
        }
        if (query.getTo() != null) {
            spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), query.getTo()));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> auditPage = auditLogRepository.findAll(spec, pageable);
        List<AuditLogDTO> content = auditPage.getContent().stream()
                .map(log -> auditLogMapper.toDto(log, privileged))
                .toList();

        PageResponse<AuditLogDTO> response = new PageResponse<>();
        response.setContent(content);
        response.setTotalElements(auditPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private boolean isPrivileged(String role) {
        return "admin".equals(role) || "nurse_leader".equals(role);
    }

    private String normalizeRole(String role) {
        return RoleMapper.normalizeRole(role);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
