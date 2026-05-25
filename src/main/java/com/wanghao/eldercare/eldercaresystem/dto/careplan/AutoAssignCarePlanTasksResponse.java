package com.wanghao.eldercare.eldercaresystem.dto.careplan;

public class AutoAssignCarePlanTasksResponse {

    private Long carePlanId;
    private int assignedCount;
    private int unassignedCount;
    private String message;

    public Long getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(Long carePlanId) {
        this.carePlanId = carePlanId;
    }

    public int getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(int assignedCount) {
        this.assignedCount = assignedCount;
    }

    public int getUnassignedCount() {
        return unassignedCount;
    }

    public void setUnassignedCount(int unassignedCount) {
        this.unassignedCount = unassignedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
