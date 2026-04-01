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
import java.time.LocalDateTime;

public class CarePlanChangeDTO {
    private Long changeId;
    private Long elderId;
    private Long currentPlanId;
    private String changeType;
    private Long requestedBy;
    private String status;
    private String reason;
    private String proposedTitle;
    private String proposedContentJson;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
    private Long newPlanId;
    private Integer generatedTaskCount;

    public static CarePlanChangeDTO from(CarePlanChangeRequest entity) {
        CarePlanChangeDTO dto = new CarePlanChangeDTO();
        dto.setChangeId(entity.getChangeId());
        dto.setElderId(entity.getElderId());
        dto.setCurrentPlanId(entity.getCurrentPlanId());
        dto.setChangeType(entity.getChangeType());
        dto.setRequestedBy(entity.getRequestedBy());
        dto.setStatus(entity.getStatus());
        dto.setReason(entity.getReason());
        dto.setProposedTitle(entity.getProposedTitle());
        dto.setProposedContentJson(entity.getProposedContentJson());
        dto.setApprovedBy(entity.getApprovedBy());
        dto.setApprovedAt(entity.getApprovedAt());
        dto.setRejectedBy(entity.getRejectedBy());
        dto.setRejectedAt(entity.getRejectedAt());
        dto.setRejectReason(entity.getRejectReason());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getChangeId() {
        return changeId;
    }

    public void setChangeId(Long changeId) {
        this.changeId = changeId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getCurrentPlanId() {
        return currentPlanId;
    }

    public void setCurrentPlanId(Long currentPlanId) {
        this.currentPlanId = currentPlanId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Long getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Long requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getProposedTitle() {
        return proposedTitle;
    }

    public void setProposedTitle(String proposedTitle) {
        this.proposedTitle = proposedTitle;
    }

    public String getProposedContentJson() {
        return proposedContentJson;
    }

    public void setProposedContentJson(String proposedContentJson) {
        this.proposedContentJson = proposedContentJson;
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

    public Long getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(Long rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getNewPlanId() {
        return newPlanId;
    }

    public void setNewPlanId(Long newPlanId) {
        this.newPlanId = newPlanId;
    }

    public Integer getGeneratedTaskCount() {
        return generatedTaskCount;
    }

    public void setGeneratedTaskCount(Integer generatedTaskCount) {
        this.generatedTaskCount = generatedTaskCount;
    }
}
