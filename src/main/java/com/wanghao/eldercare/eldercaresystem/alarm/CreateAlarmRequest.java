package com.wanghao.eldercare.eldercaresystem.alarm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateAlarmRequest {

    @NotNull(message = "elderId 不能为空")
    private Long elderId;

    private Long roomId;
    private Long bedId;

    @NotBlank(message = "alarmType 不能为空")
    private String alarmType;

    @NotBlank(message = "severity 不能为空")
    private String severity;

    @NotBlank(message = "source 不能为空")
    private String source;

    private String locationText;
    private String note;
    private String attachmentsJson;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocationText() {
        return locationText;
    }

    public void setLocationText(String locationText) {
        this.locationText = locationText;
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
