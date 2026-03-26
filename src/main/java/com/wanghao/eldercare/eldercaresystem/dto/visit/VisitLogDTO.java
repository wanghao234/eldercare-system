package com.wanghao.eldercare.eldercaresystem.dto.visit;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.visit.*;
import com.wanghao.eldercare.eldercaresystem.entity.visit.*;
import com.wanghao.eldercare.eldercaresystem.mapper.visit.*;
import com.wanghao.eldercare.eldercaresystem.service.visit.*;
import java.time.LocalDateTime;

public class VisitLogDTO {
    private Long logId;
    private Long requestId;
    private String action;
    private Long actorId;
    private LocalDateTime actionTime;
    private String comment;
    private String extraJson;

    public static VisitLogDTO from(VisitRequestLog log) {
        VisitLogDTO dto = new VisitLogDTO();
        dto.setLogId(log.getLogId());
        dto.setRequestId(log.getRequestId());
        dto.setAction(log.getAction());
        dto.setActorId(log.getActorId());
        dto.setActionTime(log.getActionTime());
        dto.setComment(log.getComment());
        dto.setExtraJson(log.getExtraJson());
        return dto;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
    }
}
