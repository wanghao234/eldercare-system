package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class AiCarePlanGenerateRequest {

    @NotNull(message = "elderId 不能为空")
    private Long elderId;

    private LocalDate startDate;

    private LocalDate endDate;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
