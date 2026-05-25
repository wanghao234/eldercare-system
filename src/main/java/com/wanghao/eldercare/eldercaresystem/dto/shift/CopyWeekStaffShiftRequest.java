package com.wanghao.eldercare.eldercaresystem.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CopyWeekStaffShiftRequest {
    @NotNull(message = "sourceWeekStart 不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate sourceWeekStart;
    @NotNull(message = "targetWeekStart 不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetWeekStart;

    public LocalDate getSourceWeekStart() {
        return sourceWeekStart;
    }

    public void setSourceWeekStart(LocalDate sourceWeekStart) {
        this.sourceWeekStart = sourceWeekStart;
    }

    public LocalDate getTargetWeekStart() {
        return targetWeekStart;
    }

    public void setTargetWeekStart(LocalDate targetWeekStart) {
        this.targetWeekStart = targetWeekStart;
    }
}
