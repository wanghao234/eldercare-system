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
import java.util.List;

public class WfTaskDTO {
    private Long wfTaskId;
    private String externalTaskId;
    private Long instanceId;
    private String processKey;
    private String bizType;
    private Long bizId;
    private String nodeKey;
    private String nodeName;
    private String taskName;
    private Long assigneeId;
    private String assigneeName;
    private String candidateRole;
    private String status;
    private Integer priority;
    private LocalDateTime dueAt;
    private LocalDateTime claimedAt;
    private LocalDateTime completedAt;
    private String comment;
    private List<WfTaskActionDTO> actions;

    public static WfTaskDTO from(WfTask task) {
        WfTaskDTO dto = new WfTaskDTO();
        dto.setWfTaskId(task.getWfTaskId());
        dto.setExternalTaskId(task.getExternalTaskId());
        dto.setInstanceId(task.getInstanceId());
        dto.setNodeKey(task.getNodeKey());
        dto.setNodeName(task.getTaskName());
        dto.setTaskName(task.getTaskName());
        dto.setAssigneeId(task.getAssigneeId());
        dto.setCandidateRole(task.getCandidateRole());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setDueAt(task.getDueAt());
        dto.setClaimedAt(task.getClaimedAt());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setComment(task.getComment());
        return dto;
    }

    public Long getWfTaskId() { return wfTaskId; }
    public void setWfTaskId(Long wfTaskId) { this.wfTaskId = wfTaskId; }
    public String getExternalTaskId() { return externalTaskId; }
    public void setExternalTaskId(String externalTaskId) { this.externalTaskId = externalTaskId; }
    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public String getProcessKey() { return processKey; }
    public void setProcessKey(String processKey) { this.processKey = processKey; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getNodeKey() { return nodeKey; }
    public void setNodeKey(String nodeKey) { this.nodeKey = nodeKey; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
    public String getCandidateRole() { return candidateRole; }
    public void setCandidateRole(String candidateRole) { this.candidateRole = candidateRole; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public List<WfTaskActionDTO> getActions() { return actions; }
    public void setActions(List<WfTaskActionDTO> actions) { this.actions = actions; }
}
