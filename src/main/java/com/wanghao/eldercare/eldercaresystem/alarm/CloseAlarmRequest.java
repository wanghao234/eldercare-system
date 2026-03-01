package com.wanghao.eldercare.eldercaresystem.alarm;

import jakarta.validation.constraints.NotBlank;

public class CloseAlarmRequest extends AlarmActionRequest {

    @NotBlank(message = "closeReason 不能为空")
    private String closeReason;

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }
}
