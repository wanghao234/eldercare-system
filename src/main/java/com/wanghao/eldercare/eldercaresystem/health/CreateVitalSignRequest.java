package com.wanghao.eldercare.eldercaresystem.health;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateVitalSignRequest {
    @NotNull
    private Long elderId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;
    private Integer heartRate;
    private Integer systolicBp;
    private Integer diastolicBp;
    private Integer spo2;
    private Double temperature;
    private Double bloodGlucose;
    private String source;
    private String note;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }
    public Integer getSystolicBp() { return systolicBp; }
    public void setSystolicBp(Integer systolicBp) { this.systolicBp = systolicBp; }
    public Integer getDiastolicBp() { return diastolicBp; }
    public void setDiastolicBp(Integer diastolicBp) { this.diastolicBp = diastolicBp; }
    public Integer getSpo2() { return spo2; }
    public void setSpo2(Integer spo2) { this.spo2 = spo2; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getBloodGlucose() { return bloodGlucose; }
    public void setBloodGlucose(Double bloodGlucose) { this.bloodGlucose = bloodGlucose; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
