package com.wanghao.eldercare.eldercaresystem.dto.activity;

public class ActivityAiUploadResponse {

    private String originalText;
    private Integer operatorId;
    private String operatorRole;
    private String fileName;
    private Long fileSize;
    private java.util.List<AiActivityFormVO> activityForms;

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public java.util.List<AiActivityFormVO> getActivityForms() {
        return activityForms;
    }

    public void setActivityForms(java.util.List<AiActivityFormVO> activityForms) {
        this.activityForms = activityForms;
    }
}
