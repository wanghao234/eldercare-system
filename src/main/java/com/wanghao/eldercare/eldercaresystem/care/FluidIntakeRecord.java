package com.wanghao.eldercare.eldercaresystem.care;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fluid_intake_records")
public class FluidIntakeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fluid_id")
    private Long fluidId;

    @Column(name = "elder_id")
    private Long elderId;

    @Column(name = "drink_type")
    private String drinkType;

    @Column(name = "volume_ml")
    private Integer volumeMl;

    @Column(name = "note")
    private String note;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "record_time")
    private LocalDateTime recordTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getFluidId() { return fluidId; }
    public void setFluidId(Long fluidId) { this.fluidId = fluidId; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getDrinkType() { return drinkType; }
    public void setDrinkType(String drinkType) { this.drinkType = drinkType; }
    public Integer getVolumeMl() { return volumeMl; }
    public void setVolumeMl(Integer volumeMl) { this.volumeMl = volumeMl; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
