package com.wanghao.eldercare.eldercaresystem.careplan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "care_plans")
public class CarePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_plan_id")
    private Long carePlanId;

    @Column(name = "elder_id", nullable = false)
    private Long elderId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "care_time", length = 64)
    private String careTime;

    @Column(name = "care_content", columnDefinition = "TEXT")
    private String careContent;

    @Column(name = "medication_reminder", columnDefinition = "TEXT")
    private String medicationReminder;

    @Column(name = "diet_plan", columnDefinition = "TEXT")
    private String dietPlan;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "record_time", insertable = false, updatable = false)
    private LocalDateTime recordTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Backward-compatible bridge for existing service logic.
    public String getPlanTitle() {
        return this.careTime;
    }

    public void setPlanTitle(String planTitle) {
        this.careTime = planTitle;
    }

    public String getPlanContentJson() {
        return this.careContent;
    }

    public void setPlanContentJson(String planContentJson) {
        this.careContent = planContentJson;
    }

    public LocalDate getEffectiveDate() {
        return this.startDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.startDate = effectiveDate;
    }
}
