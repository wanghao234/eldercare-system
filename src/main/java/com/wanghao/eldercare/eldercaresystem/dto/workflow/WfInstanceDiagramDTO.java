package com.wanghao.eldercare.eldercaresystem.dto.workflow;

import java.util.List;

public class WfInstanceDiagramDTO {
    private Long instanceId;
    private String processKey;
    private String bizType;
    private Long bizId;
    private String status;
    private String bpmnXml;
    private List<String> activeNodeKeys;
    private List<String> completedNodeKeys;
    private List<WfTaskDTO> taskNodes;
    private List<WfTaskActionDTO> actionLogs;

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
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    public List<String> getActiveNodeKeys() { return activeNodeKeys; }
    public void setActiveNodeKeys(List<String> activeNodeKeys) { this.activeNodeKeys = activeNodeKeys; }
    public List<String> getCompletedNodeKeys() { return completedNodeKeys; }
    public void setCompletedNodeKeys(List<String> completedNodeKeys) { this.completedNodeKeys = completedNodeKeys; }
    public List<WfTaskDTO> getTaskNodes() { return taskNodes; }
    public void setTaskNodes(List<WfTaskDTO> taskNodes) { this.taskNodes = taskNodes; }
    public List<WfTaskActionDTO> getActionLogs() { return actionLogs; }
    public void setActionLogs(List<WfTaskActionDTO> actionLogs) { this.actionLogs = actionLogs; }
}
