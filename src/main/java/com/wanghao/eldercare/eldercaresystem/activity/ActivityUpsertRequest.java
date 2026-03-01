package com.wanghao.eldercare.eldercaresystem.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ActivityUpsertRequest {

    @NotBlank
    @Size(max = 128)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull
    private LocalDateTime activityTime;

    @Size(max = 128)
    private String location;

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
}

