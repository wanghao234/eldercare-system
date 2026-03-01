package com.wanghao.eldercare.eldercaresystem.alarm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "alarm_action_logs")
public class AlarmActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "alarm_id", nullable = false)
    private Long alarmId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(Long alarmId) {
        this.alarmId = alarmId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getAttachmentsJson() {
        return attachmentsJson;
    }

    public void setAttachmentsJson(String attachmentsJson) {
        this.attachmentsJson = attachmentsJson;
    }
}
