package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchDeleteCarePlanTasksRequest {

    @NotEmpty(message = "taskIds 不能为空")
    private List<Long> taskIds;

    public List<Long> getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(List<Long> taskIds) {
        this.taskIds = taskIds;
    }
}
