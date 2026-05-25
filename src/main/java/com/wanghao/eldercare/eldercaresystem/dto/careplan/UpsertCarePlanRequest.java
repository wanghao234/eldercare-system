package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
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
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UpsertCarePlanRequest {
    @NotNull(message = "elderId 不能为空")
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;

    // Backward-compatible aliases
    private String planTitle;
    private JsonNode planContentJson;
    private LocalDate effectiveDate;

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

    public String getPlanTitle() {
        return planTitle;
    }

    public void setPlanTitle(String planTitle) {
        this.planTitle = planTitle;
    }

    @JsonIgnore
    public String getPlanContentJson() {
        if (planContentJson == null || planContentJson.isNull()) {
            return null;
        }
        if (planContentJson.isTextual()) {
            return planContentJson.asText();
        }
        return planContentJson.toString();
    }

    public void setPlanContentJson(JsonNode planContentJson) {
        this.planContentJson = planContentJson;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
}
