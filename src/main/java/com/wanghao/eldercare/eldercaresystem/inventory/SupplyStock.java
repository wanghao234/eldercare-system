package com.wanghao.eldercare.eldercaresystem.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supply_stocks")
public class SupplyStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long stockId;

    @Column(name = "supply_item_id", nullable = false)
    private Long supplyItemId;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "min_threshold", nullable = false)
    private BigDecimal minThreshold;

    @Column(name = "location")
    private String location;

    @Column(name = "updated_at", nullable = false)
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
