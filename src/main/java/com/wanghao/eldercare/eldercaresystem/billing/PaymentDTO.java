package com.wanghao.eldercare.eldercaresystem.billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDTO {
    private Long paymentId;
    private BigDecimal amount;
    private String method;
    private String transactionNo;
    private String status;
    private LocalDateTime paidAt;

    public static PaymentDTO from(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(payment.getPaymentId());
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getMethod());
        dto.setTransactionNo(payment.getTransactionNo());
        dto.setStatus(payment.getStatus());
        dto.setPaidAt(payment.getPaidAt());
        return dto;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
