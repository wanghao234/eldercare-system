package com.wanghao.eldercare.eldercaresystem.dto.activity;

import java.util.List;

public class ActivityBatchSignupRequest {

    private List<Long> elderIds;

    public List<Long> getElderIds() {
        return elderIds;
    }

    public void setElderIds(List<Long> elderIds) {
        this.elderIds = elderIds;
    }
}

