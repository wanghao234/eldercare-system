package com.wanghao.eldercare.eldercaresystem.ws;

import com.wanghao.eldercare.eldercaresystem.alarm.Alarm;

import java.time.LocalDateTime;

public class AlarmWsData {
    private Long alarmId;
    private Long elderId;
    private String alarmType;
    private String severity;
    private String status;
    private LocalDateTime createdAt;

    public static AlarmWsData from(Alarm alarm) {
        AlarmWsData data = new AlarmWsData();
        data.setAlarmId(alarm.getAlarmId());
        data.setElderId(alarm.getElderId());
        data.setAlarmType(alarm.getAlarmType());
        data.setSeverity(alarm.getSeverity());
        data.setStatus(alarm.getStatus());
        data.setCreatedAt(alarm.getCreatedAt());
        return data;
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
}
