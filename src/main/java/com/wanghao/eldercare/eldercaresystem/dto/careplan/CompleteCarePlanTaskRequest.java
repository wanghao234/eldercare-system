package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import jakarta.validation.constraints.NotBlank;

public class CompleteCarePlanTaskRequest {

    @NotBlank(message = "executionResult 不能为空")
    private String executionResult;

    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String executionResult) {
        this.executionResult = executionResult;
    }
}
