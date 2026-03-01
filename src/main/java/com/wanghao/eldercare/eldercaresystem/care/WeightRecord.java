package com.wanghao.eldercare.eldercaresystem.care;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "weight_records")
public class WeightRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weight_id")
    private Long weightId;

    @Column(name = "elder_id")
    private Long elderId;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "measure_ctx")
    private String measureCtx;

    @Column(name = "note")
    private String note;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "record_time")
    private LocalDateTime recordTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getWeightId() { return weightId; }
    public void setWeightId(Long weightId) { this.weightId = weightId; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public String getMeasureCtx() { return measureCtx; }
    public void setMeasureCtx(String measureCtx) { this.measureCtx = measureCtx; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
