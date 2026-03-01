package com.wanghao.eldercare.eldercaresystem.billing;

import java.math.BigDecimal;

public class BillItemDTO {
    private Long billItemId;
    private Long feeItemId;
    private String itemName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String note;

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
