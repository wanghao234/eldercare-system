package com.wanghao.eldercare.eldercaresystem.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogWriter auditLogWriter;
    private final CurrentUserUtil currentUserUtil;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogWriter auditLogWriter,
                        CurrentUserUtil currentUserUtil,
                        ObjectMapper objectMapper) {
        this.auditLogWriter = auditLogWriter;
        this.currentUserUtil = currentUserUtil;
        this.objectMapper = objectMapper;
    }

    public void logSuccess(String action, String entityType, Long entityId, Map<String, Object> detailMap) {
        AuditLog auditLog = buildAuditLog(action, entityType, entityId, detailMap, "SUCCESS", ErrorCode.SUCCESS, null);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeWrite(auditLog);
                }
            });
            return;
        }
        safeWrite(auditLog);
    }

    public void logFail(String action, String entityType, Long entityId, Map<String, Object> detailMap) {
        AuditLog auditLog = buildAuditLog(action, entityType, entityId, detailMap, "FAIL",
                asString(detailMap.getOrDefault("errorCode", ErrorCode.SYSTEM_ERROR)),
                asString(detailMap.get("errorMessage")));
        safeWrite(auditLog);
    }

    public void logTransition(String entityType,
                              Long entityId,
                              String fromStatus,
                              String toStatus,
                              String action,
                              Map<String, Object> extraMap) {
        Map<String, Object> detail = extraMap == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extraMap);
        detail.put("fromStatus", fromStatus);
        detail.put("toStatus", toStatus);
        logSuccess(action == null || action.isBlank() ? AuditAction.TRANSITION : action, entityType, entityId, detail);
    }

    public boolean isFailureAlreadyLogged(HttpServletRequest request) {
        return request != null && Boolean.TRUE.equals(request.getAttribute(AuditConstants.AUDIT_FAILURE_LOGGED_ATTR));
    }

    public void markFailureLogged(HttpServletRequest request) {
        if (request != null) {
            request.setAttribute(AuditConstants.AUDIT_FAILURE_LOGGED_ATTR, Boolean.TRUE);
        }
    }

    public void logFailIfAbsent(HttpServletRequest request,
                                String action,
                                String entityType,
                                Long entityId,
                                String errorCode,
                                String errorMessage,
                                Map<String, Object> extra) {
        if (isFailureAlreadyLogged(request)) {
            return;
        }
        Map<String, Object> detail = extra == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extra);
        detail.put("errorCode", errorCode);
        detail.put("errorMessage", errorMessage);
        logFail(action, entityType, entityId, detail);
        markFailureLogged(request);
    }

    private AuditLog buildAuditLog(String action,
                                   String entityType,
                                   Long entityId,
                                   Map<String, Object> detailMap,
                                   String result,
                                   String errorCode,
                                   String errorMessage) {
        HttpServletRequest request = currentRequest();
        Map<String, Object> detail = detailMap == null ? new LinkedHashMap<>() : new LinkedHashMap<>(detailMap);
        detail.put("traceId", resolveTraceId(request));
        detail.put("endpoint", endpoint(request));
        detail.put("method", request == null ? null : request.getMethod());
        detail.put("path", request == null ? null : request.getRequestURI());
        detail.put("result", result);
        detail.put("errorCode", errorCode);
        detail.put("errorMessage", errorMessage);

        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType == null || entityType.isBlank() ? "unknown" : entityType);
        auditLog.setEntityId(entityId);
        auditLog.setUserId(resolveUserId());
        auditLog.setIp(resolveIp(request));
        auditLog.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLog.setDetailJson(toJson(detail));
        return auditLog;
    }

    private void safeWrite(AuditLog auditLog) {
        try {
            auditLogWriter.save(auditLog);
        } catch (Exception ex) {
            log.warn("failed to write audit log: {}", ex.getMessage());
        }
    }

    private Long resolveUserId() {
        try {
            CurrentUser currentUser = currentUserUtil.getCurrentUser();
            return currentUser.getUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        return attrs.getRequest();
    }

    private String resolveTraceId(HttpServletRequest request) {
        String fromMdc = MDC.get(AuditConstants.TRACE_ID_MDC_KEY);
        if (fromMdc != null && !fromMdc.isBlank()) {
            return fromMdc;
        }
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute(AuditConstants.TRACE_ID_REQUEST_ATTR);
        return attr == null ? request.getHeader(AuditConstants.TRACE_ID_HEADER) : String.valueOf(attr);
    }

    private String endpoint(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getMethod() + " " + request.getRequestURI();
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialize_failed\"}";
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

