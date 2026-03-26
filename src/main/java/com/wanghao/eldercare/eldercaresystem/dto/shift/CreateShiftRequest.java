package com.wanghao.eldercare.eldercaresystem.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.shift.*;
import com.wanghao.eldercare.eldercaresystem.entity.shift.*;
import com.wanghao.eldercare.eldercaresystem.mapper.shift.*;
import com.wanghao.eldercare.eldercaresystem.service.shift.*;
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
