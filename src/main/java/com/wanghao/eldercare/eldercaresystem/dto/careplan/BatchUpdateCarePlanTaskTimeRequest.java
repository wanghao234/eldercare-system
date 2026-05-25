package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class BatchUpdateCarePlanTaskTimeRequest {

    @NotEmpty(message = "taskIds 不能为空")
    private List<Long> taskIds;

    @NotNull(message = "scheduledDate 不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduledDate;

    @NotNull(message = "scheduledTime 不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime scheduledTime;

    public List<Long> getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(List<Long> taskIds) {
        this.taskIds = taskIds;
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
}
