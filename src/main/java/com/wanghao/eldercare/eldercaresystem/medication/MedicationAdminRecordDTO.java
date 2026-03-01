package com.wanghao.eldercare.eldercaresystem.medication;

import java.time.LocalDateTime;

public class MedicationAdminRecordDTO {
    private Long recordId;
    private Long elderId;
    private Long medicationId;
    private Long planId;
    private LocalDateTime administeredTime;
    private Long administeredBy;
    private String status;
    private String dosage;
    private String note;
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
