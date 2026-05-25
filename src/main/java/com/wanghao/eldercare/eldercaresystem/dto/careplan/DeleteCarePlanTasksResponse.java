package com.wanghao.eldercare.eldercaresystem.dto.careplan;

public class DeleteCarePlanTasksResponse {

    private int deletedCount;
    private String message;

    public int getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
