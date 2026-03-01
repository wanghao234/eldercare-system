package com.wanghao.eldercare.eldercaresystem.careplan;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CarePlanDTO {
    private Long carePlanId;
    private Long elderId;
    private Integer version;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String careTime;
    private String careContent;
    private String medicationReminder;
    private String dietPlan;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime recordTime;
    private LocalDateTime updatedAt;

    // Backward-compatible aliases
    private String planTitle;
    private String planContentJson;
    private LocalDate effectiveDate;
    private Long createdBy;
    private LocalDateTime createdAt;

    public static CarePlanDTO from(CarePlan plan) {
        CarePlanDTO dto = new CarePlanDTO();
        dto.setCarePlanId(plan.getCarePlanId());
        dto.setElderId(plan.getElderId());
        dto.setVersion(plan.getVersion());
        dto.setStatus(plan.getStatus());
        dto.setStartDate(plan.getStartDate());
        dto.setEndDate(plan.getEndDate());
        dto.setCareTime(plan.getCareTime());
        dto.setCareContent(plan.getCareContent());
        dto.setMedicationReminder(plan.getMedicationReminder());
        dto.setDietPlan(plan.getDietPlan());
        dto.setApprovedBy(plan.getApprovedBy());
        dto.setApprovedAt(plan.getApprovedAt());
        dto.setRecordTime(plan.getRecordTime());
        dto.setUpdatedAt(plan.getUpdatedAt());
        dto.setPlanTitle(plan.getPlanTitle());
        dto.setPlanContentJson(plan.getPlanContentJson());
        dto.setEffectiveDate(plan.getEffectiveDate());
        dto.setCreatedBy(plan.getCreatedBy());
        dto.setCreatedAt(plan.getCreatedAt());
        return dto;
    }

    public Long getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(Long carePlanId) {
        this.carePlanId = carePlanId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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

    public String getCareTime() {
        return careTime;
    }

    public void setCareTime(String careTime) {
        this.careTime = careTime;
    }

    public String getCareContent() {
        return careContent;
    }

    public void setCareContent(String careContent) {
        this.careContent = careContent;
    }

    public String getMedicationReminder() {
        return medicationReminder;
    }

    public void setMedicationReminder(String medicationReminder) {
        this.medicationReminder = medicationReminder;
    }

    public String getDietPlan() {
        return dietPlan;
    }

    public void setDietPlan(String dietPlan) {
        this.dietPlan = dietPlan;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(LocalDateTime recordTime) {
        this.recordTime = recordTime;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPlanTitle() {
        return planTitle;
    }

    public void setPlanTitle(String planTitle) {
        this.planTitle = planTitle;
    }

    public String getPlanContentJson() {
        return planContentJson;
    }

    public void setPlanContentJson(String planContentJson) {
        this.planContentJson = planContentJson;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
