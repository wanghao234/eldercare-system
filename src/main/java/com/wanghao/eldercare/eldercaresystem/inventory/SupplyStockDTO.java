package com.wanghao.eldercare.eldercaresystem.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SupplyStockDTO {
    private Long stockId;
    private Long supplyItemId;
    private String itemName;
    private BigDecimal quantity;
    private BigDecimal minThreshold;
    private String location;
    private LocalDateTime updatedAt;

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public Long getSupplyItemId() {
        return supplyItemId;
    }

    public void setSupplyItemId(Long supplyItemId) {
        this.supplyItemId = supplyItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
