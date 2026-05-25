package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CarePlanExecutionReportResponse {

    private Long carePlanId;
    private Long elderId;
    private String elderName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private long totalTaskCount;
    private long completedTaskCount;
    private long pendingTaskCount;
    private long overdueTaskCount;
    private long cancelledTaskCount;
    private double completionRate;
    private List<TypeSummary> tasksByType = new ArrayList<>();
    private List<DateSummary> tasksByDate = new ArrayList<>();
    private List<ExecutionRecord> recentExecutionRecords = new ArrayList<>();
    private List<CarePlanTaskDTO> overdueTasks = new ArrayList<>();
    private String summaryText;

    public static class TypeSummary {
        private String taskType;
        private String taskTypeLabel;
        private long totalCount;
        private long completedCount;
        private long pendingCount;
        private long overdueCount;

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public String getTaskTypeLabel() {
            return taskTypeLabel;
        }

        public void setTaskTypeLabel(String taskTypeLabel) {
            this.taskTypeLabel = taskTypeLabel;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public long getCompletedCount() {
            return completedCount;
        }

        public void setCompletedCount(long completedCount) {
            this.completedCount = completedCount;
        }

        public long getPendingCount() {
            return pendingCount;
        }

        public void setPendingCount(long pendingCount) {
            this.pendingCount = pendingCount;
        }

        public long getOverdueCount() {
            return overdueCount;
        }

        public void setOverdueCount(long overdueCount) {
            this.overdueCount = overdueCount;
        }
    }

    public static class DateSummary {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private long totalCount;
        private long completedCount;
        private long pendingCount;
        private long overdueCount;
        private long cancelledCount;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public long getCompletedCount() {
            return completedCount;
        }

        public void setCompletedCount(long completedCount) {
            this.completedCount = completedCount;
        }

        public long getPendingCount() {
            return pendingCount;
        }

        public void setPendingCount(long pendingCount) {
            this.pendingCount = pendingCount;
        }

        public long getOverdueCount() {
            return overdueCount;
        }

        public void setOverdueCount(long overdueCount) {
            this.overdueCount = overdueCount;
        }

        public long getCancelledCount() {
            return cancelledCount;
        }

        public void setCancelledCount(long cancelledCount) {
            this.cancelledCount = cancelledCount;
        }
    }

    public static class ExecutionRecord {
        private Long taskId;
        private String taskTitle;
        private String taskType;
        private String taskTypeLabel;
        private String executionResult;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime executedAt;
        private String status;

        public Long getTaskId() {
            return taskId;
        }

        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }

        public String getTaskTitle() {
            return taskTitle;
        }

        public void setTaskTitle(String taskTitle) {
            this.taskTitle = taskTitle;
        }

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public String getTaskTypeLabel() {
            return taskTypeLabel;
        }

        public void setTaskTypeLabel(String taskTypeLabel) {
            this.taskTypeLabel = taskTypeLabel;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
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

    public String getElderName() {
        return elderName;
    }

    public void setElderName(String elderName) {
        this.elderName = elderName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public long getTotalTaskCount() {
        return totalTaskCount;
    }

    public void setTotalTaskCount(long totalTaskCount) {
        this.totalTaskCount = totalTaskCount;
    }

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    public void setCompletedTaskCount(long completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }

    public long getPendingTaskCount() {
        return pendingTaskCount;
    }

    public void setPendingTaskCount(long pendingTaskCount) {
        this.pendingTaskCount = pendingTaskCount;
    }

    public long getOverdueTaskCount() {
        return overdueTaskCount;
    }

    public void setOverdueTaskCount(long overdueTaskCount) {
        this.overdueTaskCount = overdueTaskCount;
    }

    public long getCancelledTaskCount() {
        return cancelledTaskCount;
    }

    public void setCancelledTaskCount(long cancelledTaskCount) {
        this.cancelledTaskCount = cancelledTaskCount;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public List<TypeSummary> getTasksByType() {
        return tasksByType;
    }

    public void setTasksByType(List<TypeSummary> tasksByType) {
        this.tasksByType = tasksByType;
    }

    public List<DateSummary> getTasksByDate() {
        return tasksByDate;
    }

    public void setTasksByDate(List<DateSummary> tasksByDate) {
        this.tasksByDate = tasksByDate;
    }

    public List<ExecutionRecord> getRecentExecutionRecords() {
        return recentExecutionRecords;
    }

    public void setRecentExecutionRecords(List<ExecutionRecord> recentExecutionRecords) {
        this.recentExecutionRecords = recentExecutionRecords;
    }

    public List<CarePlanTaskDTO> getOverdueTasks() {
        return overdueTasks;
    }

    public void setOverdueTasks(List<CarePlanTaskDTO> overdueTasks) {
        this.overdueTasks = overdueTasks;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }
}
