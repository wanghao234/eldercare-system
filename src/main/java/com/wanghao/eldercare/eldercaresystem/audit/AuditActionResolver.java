package com.wanghao.eldercare.eldercaresystem.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AuditActionResolver {

    private static final Pattern ID_PATTERN = Pattern.compile(".*/(\\d+)(?:/.*)?$");

    public ResolvedAuditMeta resolve(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path)) {
            return new ResolvedAuditMeta(AuditAction.LOGIN_FAIL, "users", null);
        }
        if ("POST".equalsIgnoreCase(method) && "/api/alarms".equals(path)) {
            return new ResolvedAuditMeta(AuditAction.CREATE, "alarms", null);
        }
        if ("POST".equalsIgnoreCase(method) && path.matches("^/api/alarms/\\d+/(accept|arrive|close)$")) {
            return new ResolvedAuditMeta(AuditAction.TRANSITION, "alarms", extractPathId(path));
        }
        if ("POST".equalsIgnoreCase(method) && path.matches("^/api/alarms/\\d+/rectifications$")) {
            return new ResolvedAuditMeta(AuditAction.CREATE, "rectifications", null);
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("^/api/elders/\\d+/(profile|risk-assessment)$")) {
            return new ResolvedAuditMeta(AuditAction.VIEW_SENSITIVE, "elder_profile", extractPathId(path));
        }
        if ("POST".equalsIgnoreCase(method)) {
            return new ResolvedAuditMeta(AuditAction.CREATE, inferEntityType(path), extractPathId(path));
        }
        if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return new ResolvedAuditMeta(AuditAction.UPDATE, inferEntityType(path), extractPathId(path));
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return new ResolvedAuditMeta(AuditAction.DELETE, inferEntityType(path), extractPathId(path));
        }
        return new ResolvedAuditMeta(AuditAction.UPDATE, inferEntityType(path), extractPathId(path));
    }

    private String inferEntityType(String path) {
        if (path == null || path.isBlank()) {
            return "unknown";
        }
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (segment == null || segment.isBlank() || "api".equals(segment) || segment.matches("\\d+")) {
                continue;
            }
            return segment;
        }
        return "unknown";
    }

    private Long extractPathId(String path) {
        if (path == null) {
            return null;
        }
        Matcher matcher = ID_PATTERN.matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }

    public record ResolvedAuditMeta(String action, String entityType, Long entityId) {
    }
}

