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

public class WfInstanceDetailDTO {
    private Long instanceId;
    private String processKey;
    private String bizType;
    private Long bizId;
    private String status;
    private Long startedBy;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String engineType;
    private String externalInstanceId;
    private List<WfTaskDTO> tasks;

    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public String getProcessKey() { return processKey; }
    public void setProcessKey(String processKey) { this.processKey = processKey; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getStartedBy() { return startedBy; }
    public void setStartedBy(Long startedBy) { this.startedBy = startedBy; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }
    public String getExternalInstanceId() { return externalInstanceId; }
    public void setExternalInstanceId(String externalInstanceId) { this.externalInstanceId = externalInstanceId; }
    public List<WfTaskDTO> getTasks() { return tasks; }
    public void setTasks(List<WfTaskDTO> tasks) { this.tasks = tasks; }
}
