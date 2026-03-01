package com.wanghao.eldercare.eldercaresystem.careplan;

public class RegenerateTasksResponse {
    private Long carePlanId;
    private Integer deletedTaskCount;
    private Integer generatedTaskCount;

    public Long getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(Long carePlanId) {
        this.carePlanId = carePlanId;
    }

    public Integer getDeletedTaskCount() {
        return deletedTaskCount;
    }

    public void setDeletedTaskCount(Integer deletedTaskCount) {
        this.deletedTaskCount = deletedTaskCount;
    }

    public Integer getGeneratedTaskCount() {
        return generatedTaskCount;
    }

    public void setGeneratedTaskCount(Integer generatedTaskCount) {
        this.generatedTaskCount = generatedTaskCount;
    }
}
