package com.wanghao.eldercare.eldercaresystem.dto.rectification;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.rectification.*;
import com.wanghao.eldercare.eldercaresystem.entity.rectification.*;
import com.wanghao.eldercare.eldercaresystem.mapper.rectification.*;
import com.wanghao.eldercare.eldercaresystem.service.rectification.*;
import java.time.LocalDateTime;

public class RectificationListItemDTO {
    private Long rectificationId;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String level;
    private Long ownerId;
    private LocalDateTime dueAt;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;

    public static RectificationListItemDTO from(Rectification rectification) {
        RectificationListItemDTO dto = new RectificationListItemDTO();
        dto.setRectificationId(rectification.getRectificationId());
        dto.setSourceType(rectification.getSourceType());
        dto.setSourceId(rectification.getSourceId());
        dto.setTitle(rectification.getTitle());
        dto.setLevel(rectification.getLevel());
        dto.setOwnerId(rectification.getOwnerId());
        dto.setDueAt(rectification.getDueAt());
        dto.setStatus(rectification.getStatus());
        dto.setCreatedBy(rectification.getCreatedBy());
        dto.setCreatedAt(rectification.getCreatedAt());
        return dto;
    }

    public Long getRectificationId() {
        return rectificationId;
    }

    public void setRectificationId(Long rectificationId) {
        this.rectificationId = rectificationId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
