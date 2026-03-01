package com.wanghao.eldercare.eldercaresystem.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class SupplyStockCreateRequest {

    @NotNull
    private Long supplyItemId;

    @Size(max = 64)
    private String location;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal minThreshold;

    public Long getSupplyItemId() {
        return supplyItemId;
    }

    public void setSupplyItemId(Long supplyItemId) {
        this.supplyItemId = supplyItemId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

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
