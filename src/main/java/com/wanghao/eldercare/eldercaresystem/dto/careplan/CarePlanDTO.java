package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.*;
import com.wanghao.eldercare.eldercaresystem.service.careplan.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CarePlanDTO {
    private Long carePlanId;
    private Long elderId;
    private Integer version;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String careLevel;
    private String careTime;
    private String careContent;
    private String medicationReminder;
    private String dietPlan;
    private String healthAssessment;
    private String nursingProblem;
    private String riskTags;
    private String nursingGoal;
    private String dailyCare;
    private String medicationCare;
    private String healthMonitoring;
    private String rehabilitationActivity;
    private String psychologicalCare;
    private String safetyPrecaution;
    private String executionFrequency;
    private String evaluation;
    private Boolean aiGenerated;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime recordTime;
    private LocalDateTime updatedAt;
    private Boolean taskGenerated;
    private Integer generatedTaskCount;
    private String taskGenerateMessage;

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
        dto.setCareLevel(plan.getCareLevel());
        dto.setCareTime(plan.getCareTime());
        dto.setCareContent(plan.getCareContent());
        dto.setMedicationReminder(plan.getMedicationReminder());
        dto.setDietPlan(plan.getDietPlan());
        dto.setHealthAssessment(plan.getHealthAssessment());
        dto.setNursingProblem(plan.getNursingProblem());
        dto.setRiskTags(plan.getRiskTags());
        dto.setNursingGoal(plan.getNursingGoal());
        dto.setDailyCare(firstNonBlank(plan.getDailyCare(), plan.getCareContent()));
        dto.setMedicationCare(firstNonBlank(plan.getMedicationCare(), plan.getMedicationReminder()));
        dto.setHealthMonitoring(plan.getHealthMonitoring());
        dto.setRehabilitationActivity(plan.getRehabilitationActivity());
        dto.setPsychologicalCare(plan.getPsychologicalCare());
        dto.setSafetyPrecaution(plan.getSafetyPrecaution());
        dto.setExecutionFrequency(firstNonBlank(plan.getExecutionFrequency(), plan.getCareTime()));
        dto.setEvaluation(plan.getEvaluation());
        dto.setAiGenerated(Boolean.TRUE.equals(plan.getAiGenerated()));
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

    public Boolean getTaskGenerated() {
        return taskGenerated;
    }

    public void setTaskGenerated(Boolean taskGenerated) {
        this.taskGenerated = taskGenerated;
    }

    public Integer getGeneratedTaskCount() {
        return generatedTaskCount;
    }

    public void setGeneratedTaskCount(Integer generatedTaskCount) {
        this.generatedTaskCount = generatedTaskCount;
    }

    public String getTaskGenerateMessage() {
        return taskGenerateMessage;
    }

    public void setTaskGenerateMessage(String taskGenerateMessage) {
        this.taskGenerateMessage = taskGenerateMessage;
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

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
