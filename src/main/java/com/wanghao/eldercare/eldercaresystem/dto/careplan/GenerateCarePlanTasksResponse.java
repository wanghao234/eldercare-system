package com.wanghao.eldercare.eldercaresystem.dto.careplan;

public class GenerateCarePlanTasksResponse {

    private Long carePlanId;
    private boolean taskGenerated;
    private int generatedCount;
    private String taskGenerateMessage;

    public Long getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(Long carePlanId) {
        this.carePlanId = carePlanId;
    }

    public boolean isTaskGenerated() {
        return taskGenerated;
    }

    public void setTaskGenerated(boolean taskGenerated) {
        this.taskGenerated = taskGenerated;
    }

    public int getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(int generatedCount) {
        this.generatedCount = generatedCount;
    }

    public String getTaskGenerateMessage() {
        return taskGenerateMessage;
    }

    public void setTaskGenerateMessage(String taskGenerateMessage) {
        this.taskGenerateMessage = taskGenerateMessage;
    }
}
