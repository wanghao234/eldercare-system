package com.wanghao.eldercare.eldercaresystem.dto.visit;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.visit.*;
import com.wanghao.eldercare.eldercaresystem.entity.visit.*;
import com.wanghao.eldercare.eldercaresystem.mapper.visit.*;
import com.wanghao.eldercare.eldercaresystem.service.visit.*;
import java.time.LocalDateTime;
import java.util.List;

public class VisitDetailDTO {
    private Long requestId;
    private Long elderId;
    private Long familyId;
    private String requestType;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private String destination;
    private String reason;
    private Integer companionCount;
    private String status;
    private LocalDateTime confirmedAt;
    private Long confirmedBy;
    private LocalDateTime approvedAt;
    private Long approvedBy;
    private LocalDateTime rejectedAt;
    private Long rejectedBy;
    private String rejectReason;
    private LocalDateTime checkOutAt;
    private Long checkOutBy;
    private LocalDateTime checkInAt;
    private Long checkInBy;
    private LocalDateTime cancelledAt;
    private Long cancelledBy;
    private String cancelReason;
    private String extraJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<VisitLogDTO> logs;

    public static VisitDetailDTO from(VisitRequest request) {
        VisitDetailDTO dto = new VisitDetailDTO();
        dto.setRequestId(request.getRequestId());
        dto.setElderId(request.getElderId());
        dto.setFamilyId(request.getFamilyId());
        dto.setRequestType(request.getRequestType());
        dto.setPlannedStartAt(request.getPlannedStartAt());
        dto.setPlannedEndAt(request.getPlannedEndAt());
        dto.setDestination(request.getDestination());
        dto.setReason(request.getReason());
        dto.setCompanionCount(request.getCompanionCount());
        dto.setStatus(request.getStatus());
        dto.setConfirmedAt(request.getConfirmedAt());
        dto.setConfirmedBy(request.getConfirmedBy());
        dto.setApprovedAt(request.getApprovedAt());
        dto.setApprovedBy(request.getApprovedBy());
        dto.setRejectedAt(request.getRejectedAt());
        dto.setRejectedBy(request.getRejectedBy());
        dto.setRejectReason(request.getRejectReason());
        dto.setCheckOutAt(request.getCheckOutAt());
        dto.setCheckOutBy(request.getCheckOutBy());
        dto.setCheckInAt(request.getCheckInAt());
        dto.setCheckInBy(request.getCheckInBy());
        dto.setCancelledAt(request.getCancelledAt());
        dto.setCancelledBy(request.getCancelledBy());
        dto.setCancelReason(request.getCancelReason());
        dto.setExtraJson(request.getExtraJson());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        return dto;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public LocalDateTime getPlannedStartAt() {
        return plannedStartAt;
    }

    public void setPlannedStartAt(LocalDateTime plannedStartAt) {
        this.plannedStartAt = plannedStartAt;
    }

    public LocalDateTime getPlannedEndAt() {
        return plannedEndAt;
    }

    public void setPlannedEndAt(LocalDateTime plannedEndAt) {
        this.plannedEndAt = plannedEndAt;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getCompanionCount() {
        return companionCount;
    }

    public void setCompanionCount(Integer companionCount) {
        this.companionCount = companionCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Long getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(Long confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Long getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(Long rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getCheckOutAt() {
        return checkOutAt;
    }

    public void setCheckOutAt(LocalDateTime checkOutAt) {
        this.checkOutAt = checkOutAt;
    }

    public Long getCheckOutBy() {
        return checkOutBy;
    }

    public void setCheckOutBy(Long checkOutBy) {
        this.checkOutBy = checkOutBy;
    }

    public LocalDateTime getCheckInAt() {
        return checkInAt;
    }

    public void setCheckInAt(LocalDateTime checkInAt) {
        this.checkInAt = checkInAt;
    }

    public Long getCheckInBy() {
        return checkInBy;
    }

    public void setCheckInBy(Long checkInBy) {
        this.checkInBy = checkInBy;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Long getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(Long cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
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

    public List<VisitLogDTO> getLogs() {
        return logs;
    }

    public void setLogs(List<VisitLogDTO> logs) {
        this.logs = logs;
    }
}
