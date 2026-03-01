package com.wanghao.eldercare.eldercaresystem.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateShiftRequest {

    @NotNull(message = "shiftDate 不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate shiftDate;

    @NotBlank(message = "shiftType 不能为空")
    private String shiftType;

    @NotNull(message = "leaderId 不能为空")
    private Long leaderId;

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public Long getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(Long leaderId) {
        this.leaderId = leaderId;
    }
}
