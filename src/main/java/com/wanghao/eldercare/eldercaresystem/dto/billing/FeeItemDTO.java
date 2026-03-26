package com.wanghao.eldercare.eldercaresystem.dto.billing;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.billing.*;
import com.wanghao.eldercare.eldercaresystem.entity.billing.*;
import com.wanghao.eldercare.eldercaresystem.mapper.billing.*;
import com.wanghao.eldercare.eldercaresystem.service.billing.*;
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
