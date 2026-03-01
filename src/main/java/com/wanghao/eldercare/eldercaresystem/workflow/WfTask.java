package com.wanghao.eldercare.eldercaresystem.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "wf_tasks")
public class WfTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wf_task_id")
    private Long wfTaskId;

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "node_key", nullable = false, length = 64)
    private String nodeKey;

    @Column(name = "task_name", length = 128)
    private String taskName;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "candidate_role", length = 32)
    private String candidateRole;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "form_data_json", columnDefinition = "TEXT")
    private String formDataJson;

    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getWfTaskId() { return wfTaskId; }
    public void setWfTaskId(Long wfTaskId) { this.wfTaskId = wfTaskId; }
    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public String getNodeKey() { return nodeKey; }
    public void setNodeKey(String nodeKey) { this.nodeKey = nodeKey; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getCandidateRole() { return candidateRole; }
    public void setCandidateRole(String candidateRole) { this.candidateRole = candidateRole; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getFormDataJson() { return formDataJson; }
    public void setFormDataJson(String formDataJson) { this.formDataJson = formDataJson; }
    public String getAttachmentsJson() { return attachmentsJson; }
    public void setAttachmentsJson(String attachmentsJson) { this.attachmentsJson = attachmentsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
