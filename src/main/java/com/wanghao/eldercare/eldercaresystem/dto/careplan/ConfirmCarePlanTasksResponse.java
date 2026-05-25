package com.wanghao.eldercare.eldercaresystem.dto.careplan;

public class ConfirmCarePlanTasksResponse {

    private Long carePlanId;
    private int confirmedTaskCount;
    private String message;

    public Long getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(Long carePlanId) {
        this.carePlanId = carePlanId;
    }

    public int getConfirmedTaskCount() {
        return confirmedTaskCount;
    }

    public void setConfirmedTaskCount(int confirmedTaskCount) {
        this.confirmedTaskCount = confirmedTaskCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
