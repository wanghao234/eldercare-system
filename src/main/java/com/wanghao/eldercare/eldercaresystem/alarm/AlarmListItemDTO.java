package com.wanghao.eldercare.eldercaresystem.alarm;

import java.time.LocalDateTime;

public class AlarmListItemDTO {
    private Long alarmId;
    private Long elderId;
    private String alarmType;
    private String severity;
    private String status;
    private LocalDateTime createdAt;
    private String locationText;

    public static AlarmListItemDTO from(Alarm alarm) {
        AlarmListItemDTO dto = new AlarmListItemDTO();
        dto.setAlarmId(alarm.getAlarmId());
        dto.setElderId(alarm.getElderId());
        dto.setAlarmType(alarm.getAlarmType());
        dto.setSeverity(alarm.getSeverity());
        dto.setStatus(alarm.getStatus());
        dto.setCreatedAt(alarm.getCreatedAt());
        dto.setLocationText(alarm.getLocationText());
        return dto;
    }

    public Long getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(Long alarmId) {
        this.alarmId = alarmId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLocationText() {
        return locationText;
    }

    public void setLocationText(String locationText) {
        this.locationText = locationText;
    }
}
