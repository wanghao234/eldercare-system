package com.wanghao.eldercare.eldercaresystem.task;

import jakarta.validation.constraints.NotNull;

public class ReassignTaskRequest {
    @NotNull(message = "assignedTo不能为空")
    private Long assignedTo;
    private String comment;

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
