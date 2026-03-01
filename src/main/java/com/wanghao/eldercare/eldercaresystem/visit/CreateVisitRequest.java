package com.wanghao.eldercare.eldercaresystem.visit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateVisitRequest {

    @NotNull(message = "elderId 不能为空")
    private Long elderId;

    @NotBlank(message = "requestType 不能为空")
    private String requestType;

    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private String destination;
    private String reason;
    private Integer companionCount;
    private JsonNode extraJson;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
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

    public JsonNode getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(JsonNode extraJson) {
        this.extraJson = extraJson;
    }
}
