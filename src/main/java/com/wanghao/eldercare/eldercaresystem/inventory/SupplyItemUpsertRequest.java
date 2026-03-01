package com.wanghao.eldercare.eldercaresystem.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupplyItemUpsertRequest {

    @NotBlank
    @Size(max = 128)
    private String itemName;

    @NotBlank
    @Size(max = 32)
    private String category;

    @Size(max = 16)
    private String unit;

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
}
