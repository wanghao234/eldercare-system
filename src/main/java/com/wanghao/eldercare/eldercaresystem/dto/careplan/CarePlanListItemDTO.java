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
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime planUpdatedAt;
    private Long pendingChangeId;
    private String pendingChangeStatus;
    private String pendingChangeType;
    private boolean requiresDoctorReview;

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
}
