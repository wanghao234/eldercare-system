package com.wanghao.eldercare.eldercaresystem.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BillSummaryDTO {
    private Long billId;
    private Long elderId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime generatedAt;
    private LocalDate dueDate;

    public static BillSummaryDTO from(Bill bill) {
        BillSummaryDTO dto = new BillSummaryDTO();
        dto.setBillId(bill.getBillId());
        dto.setElderId(bill.getElderId());
        dto.setPeriodStart(bill.getPeriodStart());
        dto.setPeriodEnd(bill.getPeriodEnd());
        dto.setTotalAmount(bill.getTotalAmount());
        dto.setStatus(bill.getStatus());
        dto.setGeneratedAt(bill.getGeneratedAt());
        dto.setDueDate(bill.getDueDate());
        return dto;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
