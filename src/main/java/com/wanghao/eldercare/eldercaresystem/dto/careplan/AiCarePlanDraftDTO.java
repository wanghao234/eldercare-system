package com.wanghao.eldercare.eldercaresystem.dto.careplan;

public class AiCarePlanDraftDTO {

    private String careLevel;
    private String healthAssessment;
    private String nursingProblem;
    private String riskTags;
    private String nursingGoal;
    private String dailyCare;
    private String dietPlan;
    private String medicationCare;
    private String healthMonitoring;
    private String rehabilitationActivity;
    private String psychologicalCare;
    private String safetyPrecaution;
    private String executionFrequency;
    private String evaluation;
    private Boolean aiGenerated;

    public String getCareLevel() {
        return careLevel;
    }

    public void setCareLevel(String careLevel) {
        this.careLevel = careLevel;
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

    public String getDietPlan() {
        return dietPlan;
    }

    public void setDietPlan(String dietPlan) {
        this.dietPlan = dietPlan;
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
}
