package com.wanghao.eldercare.eldercaresystem.medication;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateMedicationAdminRecordRequest {

    @NotNull(message = "elderId不能为空")
    private Long elderId;

    @NotNull(message = "medicationId不能为空")
    private Long medicationId;

    private Long planId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime administeredTime;

    @NotBlank(message = "status不能为空")
    private String status;

    private String dosage;
    private String note;

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
}
