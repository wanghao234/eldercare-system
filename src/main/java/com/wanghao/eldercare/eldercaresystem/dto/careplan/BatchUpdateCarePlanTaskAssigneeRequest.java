package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BatchUpdateCarePlanTaskAssigneeRequest {

    @NotEmpty(message = "taskIds 不能为空")
    private List<Long> taskIds;

    @NotNull(message = "assignedNurseId 不能为空")
    private Long assignedNurseId;

    public List<Long> getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(List<Long> taskIds) {
        this.taskIds = taskIds;
    }

    public Long getAssignedNurseId() {
        return assignedNurseId;
    }

    public void setAssignedNurseId(Long assignedNurseId) {
        this.assignedNurseId = assignedNurseId;
    }
}
