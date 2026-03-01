package com.wanghao.eldercare.eldercaresystem.shift;

import jakarta.validation.constraints.NotBlank;

public class CloseShiftRequest {

    @NotBlank(message = "from 不能为空")
    private String from;

    @NotBlank(message = "to 不能为空")
    private String to;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
