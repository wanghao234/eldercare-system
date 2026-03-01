package com.wanghao.eldercare.eldercaresystem.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SupplyStockUpdateRequest {

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal minThreshold;

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(BigDecimal minThreshold) {
        this.minThreshold = minThreshold;
    }
}
