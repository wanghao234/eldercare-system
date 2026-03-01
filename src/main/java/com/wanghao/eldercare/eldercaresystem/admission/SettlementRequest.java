package com.wanghao.eldercare.eldercaresystem.admission;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SettlementRequest {

    @NotNull(message = "settlementAmount 不能为空")
    private BigDecimal settlementAmount;

    @NotNull(message = "refundAmount 不能为空")
    private BigDecimal refundAmount;

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
}
