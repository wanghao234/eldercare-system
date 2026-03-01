package com.wanghao.eldercare.eldercaresystem.inventory;

public class SupplyItemDTO {
    private Long supplyItemId;
    private String itemName;
    private String category;
    private String unit;
    private Integer isActive;

    public static SupplyItemDTO from(SupplyItem item) {
        SupplyItemDTO dto = new SupplyItemDTO();
        dto.setSupplyItemId(item.getSupplyItemId());
        dto.setItemName(item.getItemName());
        dto.setCategory(item.getCategory());
        dto.setUnit(item.getUnit());
        dto.setIsActive(item.getIsActive());
        return dto;
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

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }
}
