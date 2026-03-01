package com.wanghao.eldercare.eldercaresystem.alarm;

import java.time.LocalDateTime;

public class AlarmActionLogDTO {
    private Long logId;
    private String action;
    private Long actorId;
    private LocalDateTime actionTime;
    private String note;
    private String attachmentsJson;

    public static AlarmActionLogDTO from(AlarmActionLog log) {
        AlarmActionLogDTO dto = new AlarmActionLogDTO();
        dto.setLogId(log.getLogId());
        dto.setAction(log.getAction());
        dto.setActorId(log.getActorId());
        dto.setActionTime(log.getActionTime());
        dto.setNote(log.getNote());
        dto.setAttachmentsJson(log.getAttachmentsJson());
        return dto;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
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
