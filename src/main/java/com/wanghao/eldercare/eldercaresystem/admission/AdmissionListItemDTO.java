package com.wanghao.eldercare.eldercaresystem.admission;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdmissionListItemDTO {
    private Long admissionId;
    private Long elderId;
    private Long bedId;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;

    public static AdmissionListItemDTO from(AdmissionRecord record) {
        AdmissionListItemDTO dto = new AdmissionListItemDTO();
        dto.setAdmissionId(record.getAdmissionId());
        dto.setElderId(record.getElderId());
        dto.setBedId(record.getBedId());
        dto.setStatus(record.getStatus());
        dto.setStartDate(record.getStartDate());
        dto.setEndDate(record.getEndDate());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
