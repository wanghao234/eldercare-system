package com.wanghao.eldercare.eldercaresystem.admission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DischargeListItemDTO {
    private Long dischargeId;
    private Long admissionId;
    private Long elderId;
    private Long bedId;
    private String status;
    private LocalDate requestedDate;
    private LocalDate actualDate;
    private BigDecimal settlementAmount;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;

    public static DischargeListItemDTO from(DischargeRecord record) {
        DischargeListItemDTO dto = new DischargeListItemDTO();
        dto.setDischargeId(record.getDischargeId());
        dto.setAdmissionId(record.getAdmissionId());
        dto.setElderId(record.getElderId());
        dto.setBedId(record.getBedId());
        dto.setStatus(record.getStatus());
        dto.setRequestedDate(record.getRequestedDate());
        dto.setActualDate(record.getActualDate());
        dto.setSettlementAmount(record.getSettlementAmount());
        dto.setRefundAmount(record.getRefundAmount());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }

    public Long getDischargeId() {
        return dischargeId;
    }

    public void setDischargeId(Long dischargeId) {
        this.dischargeId = dischargeId;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }

    public LocalDate getActualDate() {
        return actualDate;
    }

    public void setActualDate(LocalDate actualDate) {
        this.actualDate = actualDate;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(BigDecimal settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
