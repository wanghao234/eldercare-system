package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.CarePlanTask;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class CarePlanTaskDTO {

    private Long taskId;
    private Long carePlanId;
    private Long elderId;
    private Long assignedNurseId;
    private String assignedNurseName;
    private String taskType;
    private String taskTitle;
    private String taskContent;
    private String frequencyDesc;
    private String suggestedTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduledDate;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime scheduledTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;
    private String taskSource;
    private String taskGroupKey;
    private String status;
    private String executionResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static CarePlanTaskDTO from(CarePlanTask task) {
        CarePlanTaskDTO dto = new CarePlanTaskDTO();
        dto.setTaskId(task.getTaskId());
        dto.setCarePlanId(task.getCarePlanId());
        dto.setElderId(task.getElderId());
        dto.setAssignedNurseId(task.getAssignedNurseId());
        dto.setTaskType(task.getTaskType());
        dto.setTaskTitle(task.getTaskTitle());
        dto.setTaskContent(task.getTaskContent());
        dto.setFrequencyDesc(task.getFrequencyDesc());
        dto.setSuggestedTime(task.getSuggestedTime());
        dto.setScheduledDate(task.getScheduledDate());
        dto.setScheduledTime(task.getScheduledTime());
        dto.setScheduledAt(task.getScheduledAt());
        dto.setTaskSource(task.getTaskSource());
        dto.setTaskGroupKey(task.getTaskGroupKey());
        dto.setStatus(task.getStatus());
        dto.setExecutionResult(task.getExecutionResult());
        dto.setExecutedAt(task.getExecutedAt());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        return dto;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(Long carePlanId) {
        this.carePlanId = carePlanId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getAssignedNurseId() {
        return assignedNurseId;
    }

    public void setAssignedNurseId(Long assignedNurseId) {
        this.assignedNurseId = assignedNurseId;
    }

    public String getAssignedNurseName() {
        return assignedNurseName;
    }

    public void setAssignedNurseName(String assignedNurseName) {
        this.assignedNurseName = assignedNurseName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

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

    public String getFrequencyDesc() {
        return frequencyDesc;
    }

    public void setFrequencyDesc(String frequencyDesc) {
        this.frequencyDesc = frequencyDesc;
    }

    public String getSuggestedTime() {
        return suggestedTime;
    }

    public void setSuggestedTime(String suggestedTime) {
        this.suggestedTime = suggestedTime;
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

    public String getTaskSource() {
        return taskSource;
    }

    public void setTaskSource(String taskSource) {
        this.taskSource = taskSource;
    }

    public String getTaskGroupKey() {
        return taskGroupKey;
    }

    public void setTaskGroupKey(String taskGroupKey) {
        this.taskGroupKey = taskGroupKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String executionResult) {
        this.executionResult = executionResult;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
