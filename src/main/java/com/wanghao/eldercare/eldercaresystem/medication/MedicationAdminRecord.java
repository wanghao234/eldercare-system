package com.wanghao.eldercare.eldercaresystem.medication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity(name = "MedicationAdminRecord")
@Table(name = "medication_admin_records")
public class MedicationAdminRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "elder_id", nullable = false)
    private Long elderId;

    @Column(name = "medication_id", nullable = false)
    private Long medicationId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "administered_time", nullable = false)
    private LocalDateTime administeredTime;

    @Column(name = "administered_by", nullable = false)
    private Long administeredBy;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "dosage")
    private String dosage;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public LocalDateTime getAdministeredTime() {
        return administeredTime;
    }

    public void setAdministeredTime(LocalDateTime administeredTime) {
        this.administeredTime = administeredTime;
    }

    public Long getAdministeredBy() {
        return administeredBy;
    }

    public void setAdministeredBy(Long administeredBy) {
        this.administeredBy = administeredBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
