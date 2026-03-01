package com.wanghao.eldercare.eldercaresystem.workflow;

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
    public List<WfTaskDTO> getTasks() { return tasks; }
    public void setTasks(List<WfTaskDTO> tasks) { this.tasks = tasks; }
}
