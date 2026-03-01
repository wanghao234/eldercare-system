package com.wanghao.eldercare.eldercaresystem.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "wf_task_action")
public class WfTaskAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "wf_task_id", nullable = false)
    private Long wfTaskId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "form_data_json", columnDefinition = "TEXT")
    private String formDataJson;

    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

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
    public String getFormDataJson() { return formDataJson; }
    public void setFormDataJson(String formDataJson) { this.formDataJson = formDataJson; }
    public String getAttachmentsJson() { return attachmentsJson; }
    public void setAttachmentsJson(String attachmentsJson) { this.attachmentsJson = attachmentsJson; }
}
