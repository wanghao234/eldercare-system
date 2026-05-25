package com.wanghao.eldercare.eldercaresystem.dto.activity;

import java.util.List;

public class ActivityAiConfirmBatchRequest {

    private Integer operatorId;
    private String operatorRole;
    private String originalText;
    private List<AiActivityFormVO> activityForms;

    public Integer getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Integer operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorRole() {
        return operatorRole;
    }

    public void setOperatorRole(String operatorRole) {
        this.operatorRole = operatorRole;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public List<AiActivityFormVO> getActivityForms() {
        return activityForms;
    }

    public void setActivityForms(List<AiActivityFormVO> activityForms) {
        this.activityForms = activityForms;
    }
}
