package com.wanghao.eldercare.eldercaresystem.billing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BillGenerateItemRequest {

    @NotNull
    private Long feeItemId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal quantity;

    public Long getFeeItemId() {
        return feeItemId;
    }

    public void setFeeItemId(Long feeItemId) {
        this.feeItemId = feeItemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
