package com.wanghao.eldercare.eldercaresystem.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateTaskRequest {
    @NotNull(message = "elderId不能为空")
    private Long elderId;

    @NotBlank(message = "taskType不能为空")
    private String taskType;

    @NotBlank(message = "title不能为空")
    private String title;

    private String description;
    private String priority;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueAt;

    @NotNull(message = "assignedTo不能为空")
    private Long assignedTo;

    private String relatedBizType;
    private Long relatedBizId;
    private Long processInstanceId;
    private Long wfTaskId;

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

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
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

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public Long getWfTaskId() {
        return wfTaskId;
    }

    public void setWfTaskId(Long wfTaskId) {
        this.wfTaskId = wfTaskId;
    }
}
