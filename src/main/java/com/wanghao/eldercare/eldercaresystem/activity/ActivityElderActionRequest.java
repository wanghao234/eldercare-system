package com.wanghao.eldercare.eldercaresystem.activity;

import jakarta.validation.constraints.NotNull;

public class ActivityElderActionRequest {

    @NotNull
    private Long elderId;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }
}

