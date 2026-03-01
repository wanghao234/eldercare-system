package com.wanghao.eldercare.eldercaresystem.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SupplyStockAdjustRequest {

    @NotNull
    private BigDecimal delta;

    @NotBlank
    private String reason;

    public BigDecimal getDelta() {
        return delta;
    }

    public void setDelta(BigDecimal delta) {
        this.delta = delta;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
