package com.wanghao.eldercare.eldercaresystem.activity;

import java.time.LocalDateTime;

public class ActivityDTO {
    private Long activityId;
    private String title;
    private String description;
    private LocalDateTime activityTime;
    private String location;
    private Long createdBy;
    private LocalDateTime createdAt;

    public static ActivityDTO from(Activity entity) {
        ActivityDTO dto = new ActivityDTO();
        dto.setActivityId(entity.getActivityId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setActivityTime(entity.getActivityTime());
        dto.setLocation(entity.getLocation());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(LocalDateTime activityTime) {
        this.activityTime = activityTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

