package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class UpdateCarePlanTaskRequest {

    @NotBlank(message = "taskTitle 不能为空")
    private String taskTitle;

    private String taskType;
    private String taskContent;
    private String frequencyDesc;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduledDate;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime scheduledTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;
    private String suggestedTime;
    private Long assignedNurseId;
    private String status;

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getTaskContent() {
        return taskContent;
    }

    public void setTaskContent(String taskContent) {
        this.taskContent = taskContent;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getFrequencyDesc() {
        return frequencyDesc;
    }

    public void setFrequencyDesc(String frequencyDesc) {
        this.frequencyDesc = frequencyDesc;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getSuggestedTime() {
        return suggestedTime;
    }

    public void setSuggestedTime(String suggestedTime) {
        this.suggestedTime = suggestedTime;
    }

    public Long getAssignedNurseId() {
        return assignedNurseId;
    }

    public void setAssignedNurseId(Long assignedNurseId) {
        this.assignedNurseId = assignedNurseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
