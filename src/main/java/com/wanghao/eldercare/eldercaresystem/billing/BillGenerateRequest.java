package com.wanghao.eldercare.eldercaresystem.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class BillGenerateRequest {

    @NotNull
    private Long elderId;

    @NotNull
    private LocalDate periodStart;

    @NotNull
    private LocalDate periodEnd;

    @Valid
    @NotEmpty
    private List<BillGenerateItemRequest> items;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public List<BillGenerateItemRequest> getItems() {
        return items;
    }

    public void setItems(List<BillGenerateItemRequest> items) {
        this.items = items;
    }
}
