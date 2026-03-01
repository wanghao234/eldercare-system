package com.wanghao.eldercare.eldercaresystem.alarm;

public class AlarmCreateResponse {
    private Long alarmId;

    public AlarmCreateResponse() {
    }

    public AlarmCreateResponse(Long alarmId) {
        this.alarmId = alarmId;
    }

    public Long getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(Long alarmId) {
        this.alarmId = alarmId;
    }
}
