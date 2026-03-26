package com.wanghao.eldercare.eldercaresystem.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.wanghao.eldercare.eldercaresystem.service.audit.AuditService;

@Aspect
@Component
public class AuditAspect {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwordHash", "password_hash", "token", "authorization", "accessToken", "refreshToken"
    );

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        HttpServletRequest request = currentRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        Long entityId = resolveEntityId(audited, args, paramNames, request);
        Map<String, Object> requestSummary = safeSummarizeRequest(audited, method, args);
        String fromStatus = safeResolveStatus(audited.fromField(), audited.fromValue(), method, args);
        String toStatus = safeResolveStatus(audited.toField(), audited.toValue(), method, args);

        try {
            Object result = joinPoint.proceed();
            Long responseEntityId = resolveEntityIdFromResponse(audited, result);
            if (responseEntityId != null) {
                entityId = responseEntityId;
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("fromStatus", fromStatus);
            detail.put("toStatus", toStatus);
            detail.put("request", requestSummary);
            detail.put("responseSummary", safeSummarizeResponse(result));
            if (audited.sensitive()) {
                detail.put("extra", Map.of("sensitive", true));
            }
            auditService.logSuccess(audited.action(), audited.entityType(), entityId, detail);
            return result;
        } catch (Throwable ex) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("fromStatus", fromStatus);
            detail.put("toStatus", toStatus);
            detail.put("request", requestSummary);
            detail.put("errorCode", resolveErrorCode(ex));
            detail.put("errorMessage", ex.getMessage());
            String failAction = audited.failAction().isBlank() ? audited.action() : audited.failAction();
            auditService.logFail(failAction, audited.entityType(), entityId, detail);
            auditService.markFailureLogged(request);
            throw ex;
        }
    }

    private Long resolveEntityId(Audited audited, Object[] args, String[] paramNames, HttpServletRequest request) {
        if (audited.entityIdArg() != null && !audited.entityIdArg().isBlank() && paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                if (audited.entityIdArg().equals(paramNames[i])) {
                    return toLong(args[i]);
                }
            }
        }
        if (request != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> vars = (Map<String, String>) request.getAttribute("org.springframework.web.servlet.HandlerMapping.uriTemplateVariables");
            if (vars != null) {
                if (audited.entityIdArg() != null && !audited.entityIdArg().isBlank() && vars.containsKey(audited.entityIdArg())) {
                    return toLong(vars.get(audited.entityIdArg()));
                }
                for (String key : List.of("id", "alarmId", "elderId", "taskId", "wfTaskId")) {
                    if (vars.containsKey(key)) {
                        return toLong(vars.get(key));
                    }
                }
            }
        }
        return null;
    }

    private Long resolveEntityIdFromResponse(Audited audited, Object result) {
        if (audited.responseIdPath() == null || audited.responseIdPath().isBlank()) {
            return null;
        }
        Object data = result;
        if (result instanceof ApiResponse<?> apiResponse) {
            data = apiResponse.getData();
        }
        if (data == null) {
            return null;
        }
        Object current = objectMapper.convertValue(data, Object.class);
        String[] paths = audited.responseIdPath().split("\\.");
        for (String path : paths) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(path);
            if (current == null) {
                return null;
            }
        }
        return toLong(current);
    }

    private String resolveStatus(String field, String fallback, Method method, Object[] args) {
        if (field != null && !field.isBlank()) {
            Object requestBody = extractRequestBody(method, args);
            if (requestBody instanceof Map<?, ?> map) {
                Object v = map.get(field);
                return v == null ? fallback : String.valueOf(v);
            }
        }
        return fallback == null || fallback.isBlank() ? null : fallback;
    }

    private String safeResolveStatus(String field, String fallback, Method method, Object[] args) {
        try {
            return resolveStatus(field, fallback, method, args);
        } catch (Exception ex) {
            return fallback == null || fallback.isBlank() ? null : fallback;
        }
    }

    private Map<String, Object> summarizeRequest(Audited audited, Method method, Object[] args) {
        Object requestBody = extractRequestBody(method, args);
        if (!(requestBody instanceof Map<?, ?> raw)) {
            return null;
        }
        Map<String, Object> source = castMap(raw);
        Map<String, Object> result = new LinkedHashMap<>();
        if (audited.requestFields().length > 0) {
            for (String field : audited.requestFields()) {
                if (source.containsKey(field)) {
                    result.put(field, maskIfSensitive(field, source.get(field)));
                }
            }
            return result;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (SENSITIVE_KEYS.contains(entry.getKey())) {
                continue;
            }
            result.put(entry.getKey(), maskIfSensitive(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> safeSummarizeRequest(Audited audited, Method method, Object[] args) {
        try {
            return summarizeRequest(audited, method, args);
        } catch (Exception ex) {
            // Never block business flow because audit summary parsing failed.
            return null;
        }
    }

    private Map<String, Object> summarizeResponse(Object result) {
        Object data = result;
        if (result instanceof ApiResponse<?> apiResponse) {
            data = apiResponse.getData();
        }
        if (data == null) {
            return null;
        }
        if (isSimpleValue(data)) {
            return Map.of("value", data);
        }
        Map<String, Object> map = castMap(objectMapper.convertValue(data, Map.class));
        Map<String, Object> summary = new LinkedHashMap<>();
        for (String key : List.of("id", "alarmId", "rectificationId", "requestId", "taskId", "wfTaskId", "status", "userId")) {
            if (map.containsKey(key)) {
                summary.put(key, map.get(key));
            }
        }
        return summary.isEmpty() ? null : summary;
    }

    private Map<String, Object> safeSummarizeResponse(Object result) {
        try {
            return summarizeResponse(result);
        } catch (Exception ex) {
            return null;
        }
    }

    private Object extractRequestBody(Method method, Object[] args) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length && i < args.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation.annotationType().equals(RequestBody.class) && args[i] != null) {
                    try {
                        return objectMapper.convertValue(args[i], Map.class);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private String resolveErrorCode(Throwable ex) {
        if (ex instanceof BusinessException businessException) {
            return businessException.getCode();
        }
        if (ex instanceof AccessDeniedException) {
            return ErrorCode.FORBIDDEN;
        }
        if (ex instanceof AuthenticationException) {
            return ErrorCode.UNAUTHORIZED;
        }
        if (ex instanceof MethodArgumentNotValidException || ex instanceof BindException || ex instanceof IllegalArgumentException) {
            return ErrorCode.BAD_REQUEST;
        }
        return ErrorCode.SYSTEM_ERROR;
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Temporal;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private Object maskIfSensitive(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (SENSITIVE_KEYS.contains(key)) {
            return "***";
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> masked = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String childKey = String.valueOf(entry.getKey());
                masked.put(childKey, maskIfSensitive(childKey, entry.getValue()));
            }
            return masked;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(v -> maskIfSensitive("", v)).toList();
        }
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            Object[] masked = new Object[len];
            for (int i = 0; i < len; i++) {
                masked[i] = maskIfSensitive("", Array.get(value, i));
            }
            return Arrays.asList(masked);
        }
        return value;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String str = String.valueOf(value);
        if (str.isBlank() || !str.matches("\\d+")) {
            return null;
        }
        return Long.valueOf(str);
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        return attrs.getRequest();
    }
}
