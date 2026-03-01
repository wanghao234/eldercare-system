package com.wanghao.eldercare.eldercaresystem.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "wf_definitions")
public class WfDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "definition_id")
    private Long definitionId;

    @Column(name = "process_key", nullable = false, length = 64)
    private String processKey;

    @Column(name = "process_name", length = 128)
    private String processName;

    @Column(name = "version")
    private Integer version;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "definition_json", columnDefinition = "TEXT")
    private String definitionJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public String getProcessKey() { return processKey; }
    public void setProcessKey(String processKey) { this.processKey = processKey; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDefinitionJson() { return definitionJson; }
    public void setDefinitionJson(String definitionJson) { this.definitionJson = definitionJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
