package com.wanghao.eldercare.eldercaresystem.dto.activity;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.activity.*;
import com.wanghao.eldercare.eldercaresystem.entity.activity.*;
import com.wanghao.eldercare.eldercaresystem.mapper.activity.*;
import com.wanghao.eldercare.eldercaresystem.service.activity.*;
import java.time.LocalDateTime;

public class ActivityParticipantDTO {
    private Long id;
    private Long activityId;
    private Long elderId;
    private String status;
    private LocalDateTime createdAt;

    public static ActivityParticipantDTO from(ActivityParticipant entity) {
        ActivityParticipantDTO dto = new ActivityParticipantDTO();
        dto.setId(entity.getId());
        dto.setActivityId(entity.getActivityId());
        dto.setElderId(entity.getElderId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

