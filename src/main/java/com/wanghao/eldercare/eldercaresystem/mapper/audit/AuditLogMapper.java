package com.wanghao.eldercare.eldercaresystem.mapper.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.audit.*;
import com.wanghao.eldercare.eldercaresystem.dto.audit.*;
import com.wanghao.eldercare.eldercaresystem.dto.audit.AuditLogDTO;
import com.wanghao.eldercare.eldercaresystem.entity.audit.*;
import com.wanghao.eldercare.eldercaresystem.service.audit.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    private static final Set<String> DETAIL_ALLOWED_FIELDS = Set.of(
            "traceId", "endpoint", "method", "path", "result", "errorCode", "errorMessage", "fromStatus", "toStatus", "extra"
    );
    private static final Set<String> REQUEST_ALLOWED_FIELDS = Set.of("id", "status", "from", "to", "entityId", "elderId", "alarmId");

    private final ObjectMapper objectMapper;

    public AuditLogMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditLogDTO toDto(AuditLog log, boolean privileged) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setLogId(log.getLogId());
        dto.setUserId(log.getUserId());
        dto.setAction(log.getAction());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setCreatedAt(log.getCreatedAt());
        dto.setDetailJson(cropDetail(log.getDetailJson()));
        if (privileged) {
            dto.setIp(log.getIp());
            dto.setUserAgent(log.getUserAgent());
        } else {
            dto.setIp(null);
            dto.setUserAgent(null);
        }
        return dto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cropDetail(String detailJsonRaw) {
        if (detailJsonRaw == null || detailJsonRaw.isBlank()) {
            return null;
        }
        Map<String, Object> source = objectMapper.convertValue(parse(detailJsonRaw), Map.class);
        Map<String, Object> target = new LinkedHashMap<>();
        for (String key : DETAIL_ALLOWED_FIELDS) {
            if (source.containsKey(key)) {
                target.put(key, source.get(key));
            }
        }
        Object requestObj = source.get("request");
        if (requestObj instanceof Map<?, ?> reqMap) {
            Map<String, Object> sanitizedRequest = new LinkedHashMap<>();
            for (String key : REQUEST_ALLOWED_FIELDS) {
                if (reqMap.containsKey(key)) {
                    sanitizedRequest.put(key, reqMap.get(key));
                }
            }
            if (!sanitizedRequest.isEmpty()) {
                target.put("request", sanitizedRequest);
            }
        }
        return target;
    }

    private Object parse(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ex) {
            return Map.of("raw", json);
        }
    }
}

