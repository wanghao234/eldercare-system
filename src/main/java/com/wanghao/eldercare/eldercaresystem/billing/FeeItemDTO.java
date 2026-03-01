package com.wanghao.eldercare.eldercaresystem.billing;

import java.math.BigDecimal;

public class FeeItemDTO {
    private Long feeItemId;
    private String itemName;
    private String category;
    private String unit;
    private BigDecimal unitPrice;
    private Integer isActive;

    public static FeeItemDTO from(FeeItem feeItem) {
        FeeItemDTO dto = new FeeItemDTO();
        dto.setFeeItemId(feeItem.getFeeItemId());
        dto.setItemName(feeItem.getItemName());
        dto.setCategory(feeItem.getCategory());
        dto.setUnit(feeItem.getUnit());
        dto.setUnitPrice(feeItem.getUnitPrice());
        dto.setIsActive(feeItem.getIsActive());
        return dto;
    }

    public Long getFeeItemId() {
        return feeItemId;
    }

    public void setFeeItemId(Long feeItemId) {
        this.feeItemId = feeItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }
}
