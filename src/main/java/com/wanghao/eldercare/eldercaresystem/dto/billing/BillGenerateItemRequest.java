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
