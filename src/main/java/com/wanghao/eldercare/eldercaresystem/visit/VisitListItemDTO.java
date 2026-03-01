package com.wanghao.eldercare.eldercaresystem.visit;

import java.time.LocalDateTime;

public class VisitListItemDTO {
    private Long requestId;
    private Long elderId;
    private Long familyId;
    private String requestType;
    private String status;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private String destination;
    private LocalDateTime createdAt;

    public static VisitListItemDTO from(VisitRequest request) {
        VisitListItemDTO dto = new VisitListItemDTO();
        dto.setRequestId(request.getRequestId());
        dto.setElderId(request.getElderId());
        dto.setFamilyId(request.getFamilyId());
        dto.setRequestType(request.getRequestType());
        dto.setStatus(request.getStatus());
        dto.setPlannedStartAt(request.getPlannedStartAt());
        dto.setPlannedEndAt(request.getPlannedEndAt());
        dto.setDestination(request.getDestination());
        dto.setCreatedAt(request.getCreatedAt());
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
