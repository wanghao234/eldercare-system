package com.wanghao.eldercare.eldercaresystem.dto.rectification;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.rectification.*;
import com.wanghao.eldercare.eldercaresystem.entity.rectification.*;
import com.wanghao.eldercare.eldercaresystem.mapper.rectification.*;
import com.wanghao.eldercare.eldercaresystem.service.rectification.*;
import java.time.LocalDateTime;

public class RectificationActionDTO {
    private Long actionId;
    private String actionType;
    private Long actorId;
    private LocalDateTime actionTime;
    private String content;
    private String attachmentsJson;
    private String extraJson;

    public static RectificationActionDTO from(RectificationAction action) {
        RectificationActionDTO dto = new RectificationActionDTO();
        dto.setActionId(action.getActionId());
        dto.setActionType(action.getActionType());
        dto.setActorId(action.getActorId());
        dto.setActionTime(action.getActionTime());
        dto.setContent(action.getContent());
        dto.setAttachmentsJson(action.getAttachmentsJson());
        dto.setExtraJson(action.getExtraJson());
        return dto;
    }

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAttachmentsJson() {
        return attachmentsJson;
    }

    public void setAttachmentsJson(String attachmentsJson) {
        this.attachmentsJson = attachmentsJson;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
    }
}
