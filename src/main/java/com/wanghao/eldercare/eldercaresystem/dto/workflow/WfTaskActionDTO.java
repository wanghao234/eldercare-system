package com.wanghao.eldercare.eldercaresystem.dto.workflow;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.workflow.*;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.workflow.*;
import java.time.LocalDateTime;

public class WfTaskActionDTO {
    private Long actionId;
    private Long wfTaskId;
    private String action;
    private Long actorId;
    private LocalDateTime actionTime;
    private String comment;

    public static WfTaskActionDTO from(WfTaskAction action) {
        WfTaskActionDTO dto = new WfTaskActionDTO();
        dto.setActionId(action.getActionId());
        dto.setWfTaskId(action.getWfTaskId());
        dto.setAction(action.getAction());
        dto.setActorId(action.getActorId());
        dto.setActionTime(action.getActionTime());
        dto.setComment(action.getComment());
        return dto;
    }

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public Long getWfTaskId() { return wfTaskId; }
    public void setWfTaskId(Long wfTaskId) { this.wfTaskId = wfTaskId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public LocalDateTime getActionTime() { return actionTime; }
    public void setActionTime(LocalDateTime actionTime) { this.actionTime = actionTime; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
