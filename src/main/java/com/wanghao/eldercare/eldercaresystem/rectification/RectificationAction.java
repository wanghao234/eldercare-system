package com.wanghao.eldercare.eldercaresystem.rectification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "rectification_actions")
public class RectificationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "rectification_id", nullable = false)
    private Long rectificationId;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(name = "extra_json", columnDefinition = "TEXT")
    private String extraJson;

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }

    public Long getRectificationId() {
        return rectificationId;
    }

    public void setRectificationId(Long rectificationId) {
        this.rectificationId = rectificationId;
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
