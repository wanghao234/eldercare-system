package com.wanghao.eldercare.eldercaresystem.entity.workflow;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.workflow.*;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.workflow.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "wf_definitions")
public class WfDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "def_id")
    private Long definitionId;

    @Column(name = "process_key", nullable = false, length = 64)
    private String processKey;

    @Column(name = "name", length = 128)
    private String processName;

    @Column(name = "version")
    private Integer version;

    @Column(name = "is_active")
    private Integer isActive;

    @Column(name = "bpmn_xml", columnDefinition = "LONGTEXT")
    private String definitionJson;

    @Column(name = "engine_type", length = 32)
    private String engineType;

    @Column(name = "external_deployment_id", length = 128)
    private String externalDeploymentId;

    @Column(name = "external_process_definition_id", length = 128)
    private String externalProcessDefinitionId;

    @Column(name = "deployment_time")
    private LocalDateTime deploymentTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Transient
    private LocalDateTime updatedAt;

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public String getProcessKey() { return processKey; }
    public void setProcessKey(String processKey) { this.processKey = processKey; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return Integer.valueOf(1).equals(isActive) ? "active" : "inactive"; }
    public void setStatus(String status) { this.isActive = "active".equalsIgnoreCase(status) ? 1 : 0; }
    public String getDefinitionJson() { return definitionJson; }
    public void setDefinitionJson(String definitionJson) { this.definitionJson = definitionJson; }
    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }
    public String getExternalDeploymentId() { return externalDeploymentId; }
    public void setExternalDeploymentId(String externalDeploymentId) { this.externalDeploymentId = externalDeploymentId; }
    public String getExternalProcessDefinitionId() { return externalProcessDefinitionId; }
    public void setExternalProcessDefinitionId(String externalProcessDefinitionId) { this.externalProcessDefinitionId = externalProcessDefinitionId; }
    public LocalDateTime getDeploymentTime() { return deploymentTime; }
    public void setDeploymentTime(LocalDateTime deploymentTime) { this.deploymentTime = deploymentTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
