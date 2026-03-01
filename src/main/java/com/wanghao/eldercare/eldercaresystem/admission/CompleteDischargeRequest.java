package com.wanghao.eldercare.eldercaresystem.admission;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CompleteDischargeRequest {

    @NotNull(message = "actualDate 不能为空")
    private LocalDate actualDate;

    public LocalDate getActualDate() {
        return actualDate;
    }

    public void setActualDate(LocalDate actualDate) {
        this.actualDate = actualDate;
    }
}
