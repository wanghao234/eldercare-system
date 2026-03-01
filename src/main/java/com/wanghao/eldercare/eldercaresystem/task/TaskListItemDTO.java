package com.wanghao.eldercare.eldercaresystem.task;

import java.time.LocalDateTime;

public class TaskListItemDTO {
    private Long taskId;
    private Long elderId;
    private String taskType;
    private String title;
    private String description;
    private String priority;
    private String status;
    private LocalDateTime dueAt;
    private Long assignedTo;
    private Long createdBy;
    private Long completedBy;
    private LocalDateTime completedAt;
    private String relatedBizType;
    private Long relatedBizId;

    public static TaskListItemDTO from(Task task) {
        TaskListItemDTO dto = new TaskListItemDTO();
        dto.setTaskId(task.getTaskId());
        dto.setElderId(task.getElderId());
        dto.setTaskType(task.getTaskType());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setPriority(task.getPriority());
        dto.setStatus(task.getStatus());
        dto.setDueAt(task.getDueAt());
        dto.setAssignedTo(task.getAssignedTo());
        dto.setCreatedBy(task.getCreatedBy());
        dto.setCompletedBy(task.getCompletedBy());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setRelatedBizType(task.getRelatedBizType());
        dto.setRelatedBizId(task.getRelatedBizId());
        return dto;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(Long completedBy) {
        this.completedBy = completedBy;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getRelatedBizType() {
        return relatedBizType;
    }

    public void setRelatedBizType(String relatedBizType) {
        this.relatedBizType = relatedBizType;
    }

    public Long getRelatedBizId() {
        return relatedBizId;
    }

    public void setRelatedBizId(Long relatedBizId) {
        this.relatedBizId = relatedBizId;
    }
}
