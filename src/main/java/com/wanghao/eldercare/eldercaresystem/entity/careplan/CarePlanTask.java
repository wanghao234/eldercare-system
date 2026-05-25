package com.wanghao.eldercare.eldercaresystem.entity.careplan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "care_plan_tasks")
public class CarePlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "care_plan_id", nullable = false)
    private Long carePlanId;

    @Column(name = "elder_id", nullable = false)
    private Long elderId;

    @Column(name = "assigned_nurse_id")
    private Long assignedNurseId;

    @Column(name = "task_type", length = 64)
    private String taskType;

    @Column(name = "task_title", nullable = false, length = 128)
    private String taskTitle;

    @Column(name = "task_content", columnDefinition = "TEXT")
    private String taskContent;

    @Column(name = "frequency_desc", length = 128)
    private String frequencyDesc;

    @Column(name = "suggested_time", length = 128)
    private String suggestedTime;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "task_source", length = 32)
    private String taskSource;

    @Column(name = "task_group_key", length = 64)
    private String taskGroupKey;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "execution_result", columnDefinition = "TEXT")
    private String executionResult;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public String getStatus() {
        return status;
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
