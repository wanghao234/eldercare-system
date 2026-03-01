package com.wanghao.eldercare.eldercaresystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.audit.AuditActionResolver;
import com.wanghao.eldercare.eldercaresystem.audit.AuditService;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final AuditActionResolver auditActionResolver;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper,
                                     AuditService auditService,
                                     AuditActionResolver auditActionResolver) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.auditActionResolver = auditActionResolver;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        AuditActionResolver.ResolvedAuditMeta meta = auditActionResolver.resolve(request);
        auditService.logFailIfAbsent(
                request,
                meta.action(),
                meta.entityType(),
                meta.entityId(),
                ErrorCode.FORBIDDEN,
                "Forbidden",
                null
        );
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ErrorCode.FORBIDDEN, "Forbidden", null)
        ));
    }
}
