package com.wanghao.eldercare.eldercaresystem.health;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vital_sign_records")
public class VitalSignRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vital_id")
    private Long vitalId;

    @Column(name = "elder_id")
    private Long elderId;

    @Column(name = "record_time")
    private LocalDateTime recordTime;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "systolic_bp")
    private Integer systolicBp;

    @Column(name = "diastolic_bp")
    private Integer diastolicBp;

    @Column(name = "spo2")
    private Integer spo2;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "blood_glucose")
    private Double bloodGlucose;

    @Column(name = "source")
    private String source;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getVitalId() { return vitalId; }
    public void setVitalId(Long vitalId) { this.vitalId = vitalId; }
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
    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
