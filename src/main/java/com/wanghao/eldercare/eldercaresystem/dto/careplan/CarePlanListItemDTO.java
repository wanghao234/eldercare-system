package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CarePlanListItemDTO {
    private Long elderId;
    private String elderUsername;
    private String elderName;
    private String elderStatus;
    private boolean hasCarePlan;
    private Long carePlanId;
    private Integer carePlanVersion;
    private String carePlanStatus;
    private String careTime;
    private String careLevel;
    private String dailyCare;
    private String medicationCare;
    private String executionFrequency;
    private String dietPlan;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime planUpdatedAt;
    private Long pendingChangeId;
    private String pendingChangeStatus;
    private String pendingChangeType;
    private boolean requiresDoctorReview;
    private String admissionStatus;
    private boolean inResidence;
    private boolean canCreateCarePlan;
    private boolean canSubmitCarePlan;
    private boolean canChangeCarePlan;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getElderUsername() {
        return elderUsername;
    }

    public void setElderUsername(String elderUsername) {
        this.elderUsername = elderUsername;
    }

    public String getElderName() {
        return elderName;
    }

    public void setElderName(String elderName) {
        this.elderName = elderName;
    }

    public String getElderStatus() {
        return elderStatus;
    }

    public void setElderStatus(String elderStatus) {
        this.elderStatus = elderStatus;
    }

    public boolean isHasCarePlan() {
        return hasCarePlan;
    }

    public void setHasCarePlan(boolean hasCarePlan) {
        this.hasCarePlan = hasCarePlan;
    }

    public Long getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(Long carePlanId) {
        this.carePlanId = carePlanId;
    }

    public Integer getCarePlanVersion() {
        return carePlanVersion;
    }

    public void setCarePlanVersion(Integer carePlanVersion) {
        this.carePlanVersion = carePlanVersion;
    }

    public String getCarePlanStatus() {
        return carePlanStatus;
    }

    public void setCarePlanStatus(String carePlanStatus) {
        this.carePlanStatus = carePlanStatus;
    }

    public String getCareTime() {
        return careTime;
    }

    public void setCareTime(String careTime) {
        this.careTime = careTime;
    }

    public String getCareLevel() {
        return careLevel;
    }

    public void setCareLevel(String careLevel) {
        this.careLevel = careLevel;
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

    public String getExecutionFrequency() {
        return executionFrequency;
    }

    public void setExecutionFrequency(String executionFrequency) {
        this.executionFrequency = executionFrequency;
    }

    public String getDietPlan() {
        return dietPlan;
    }

    public void setDietPlan(String dietPlan) {
        this.dietPlan = dietPlan;
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

    public LocalDateTime getPlanUpdatedAt() {
        return planUpdatedAt;
    }

    public void setPlanUpdatedAt(LocalDateTime planUpdatedAt) {
        this.planUpdatedAt = planUpdatedAt;
    }

    public Long getPendingChangeId() {
        return pendingChangeId;
    }

    public void setPendingChangeId(Long pendingChangeId) {
        this.pendingChangeId = pendingChangeId;
    }

    public String getPendingChangeStatus() {
        return pendingChangeStatus;
    }

    public void setPendingChangeStatus(String pendingChangeStatus) {
        this.pendingChangeStatus = pendingChangeStatus;
    }

    public String getPendingChangeType() {
        return pendingChangeType;
    }

    public void setPendingChangeType(String pendingChangeType) {
        this.pendingChangeType = pendingChangeType;
    }

    public boolean isRequiresDoctorReview() {
        return requiresDoctorReview;
    }

    public void setRequiresDoctorReview(boolean requiresDoctorReview) {
        this.requiresDoctorReview = requiresDoctorReview;
    }

    public String getAdmissionStatus() {
        return admissionStatus;
    }

    public void setAdmissionStatus(String admissionStatus) {
        this.admissionStatus = admissionStatus;
    }

    public boolean isInResidence() {
        return inResidence;
    }

    public void setInResidence(boolean inResidence) {
        this.inResidence = inResidence;
    }

    public boolean isCanCreateCarePlan() {
        return canCreateCarePlan;
    }

    public void setCanCreateCarePlan(boolean canCreateCarePlan) {
        this.canCreateCarePlan = canCreateCarePlan;
    }

    public boolean isCanSubmitCarePlan() {
        return canSubmitCarePlan;
    }

    public void setCanSubmitCarePlan(boolean canSubmitCarePlan) {
        this.canSubmitCarePlan = canSubmitCarePlan;
    }

    public boolean isCanChangeCarePlan() {
        return canChangeCarePlan;
    }

    public void setCanChangeCarePlan(boolean canChangeCarePlan) {
        this.canChangeCarePlan = canChangeCarePlan;
    }
}
