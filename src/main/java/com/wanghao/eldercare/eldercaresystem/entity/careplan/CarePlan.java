package com.wanghao.eldercare.eldercaresystem.entity.careplan;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careplan.*;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.*;
import com.wanghao.eldercare.eldercaresystem.service.careplan.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;

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

    @Column(name = "care_level", length = 32)
    private String careLevel;

    @Column(name = "care_time", length = 64)
    private String careTime;

    @Column(name = "care_content", columnDefinition = "TEXT")
    private String careContent;

    @Column(name = "medication_reminder", columnDefinition = "TEXT")
    private String medicationReminder;

    @Column(name = "diet_plan", columnDefinition = "TEXT")
    private String dietPlan;

    @Column(name = "health_assessment", columnDefinition = "TEXT")
    private String healthAssessment;

    @Column(name = "nursing_problem", columnDefinition = "TEXT")
    private String nursingProblem;

    @Column(name = "risk_tags", length = 255)
    private String riskTags;

    @Column(name = "nursing_goal", columnDefinition = "TEXT")
    private String nursingGoal;

    @Column(name = "daily_care", columnDefinition = "TEXT")
    private String dailyCare;

    @Column(name = "medication_care", columnDefinition = "TEXT")
    private String medicationCare;

    @Column(name = "health_monitoring", columnDefinition = "TEXT")
    private String healthMonitoring;

    @Column(name = "rehabilitation_activity", columnDefinition = "TEXT")
    private String rehabilitationActivity;

    @Column(name = "psychological_care", columnDefinition = "TEXT")
    private String psychologicalCare;

    @Column(name = "safety_precaution", columnDefinition = "TEXT")
    private String safetyPrecaution;

    @Column(name = "execution_frequency", length = 128)
    private String executionFrequency;

    @Column(name = "evaluation", columnDefinition = "TEXT")
    private String evaluation;

    @Column(name = "ai_generated", nullable = false)
    private Boolean aiGenerated;

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

    public String getCareLevel() {
        return careLevel;
    }

    public void setCareLevel(String careLevel) {
        this.careLevel = careLevel;
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

    public String getHealthAssessment() {
        return healthAssessment;
    }

    public void setHealthAssessment(String healthAssessment) {
        this.healthAssessment = healthAssessment;
    }

    public String getNursingProblem() {
        return nursingProblem;
    }

    public void setNursingProblem(String nursingProblem) {
        this.nursingProblem = nursingProblem;
    }

    public String getRiskTags() {
        return riskTags;
    }

    public void setRiskTags(String riskTags) {
        this.riskTags = riskTags;
    }

    public String getNursingGoal() {
        return nursingGoal;
    }

    public void setNursingGoal(String nursingGoal) {
        this.nursingGoal = nursingGoal;
    }

    public String getDailyCare() {
        return dailyCare;
    }

    public void setDailyCare(String dailyCare) {
        this.dailyCare = dailyCare;
    }

    public String getMedicationCare() {
        return medicationCare;
    }

    public void setMedicationCare(String medicationCare) {
        this.medicationCare = medicationCare;
    }

    public String getHealthMonitoring() {
        return healthMonitoring;
    }

    public void setHealthMonitoring(String healthMonitoring) {
        this.healthMonitoring = healthMonitoring;
    }

    public String getRehabilitationActivity() {
        return rehabilitationActivity;
    }

    public void setRehabilitationActivity(String rehabilitationActivity) {
        this.rehabilitationActivity = rehabilitationActivity;
    }

    public String getPsychologicalCare() {
        return psychologicalCare;
    }

    public void setPsychologicalCare(String psychologicalCare) {
        this.psychologicalCare = psychologicalCare;
    }

    public String getSafetyPrecaution() {
        return safetyPrecaution;
    }

    public void setSafetyPrecaution(String safetyPrecaution) {
        this.safetyPrecaution = safetyPrecaution;
    }

    public String getExecutionFrequency() {
        return executionFrequency;
    }

    public void setExecutionFrequency(String executionFrequency) {
        this.executionFrequency = executionFrequency;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }

    public Boolean getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
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
