package com.wanghao.eldercare.eldercaresystem.qc;

import java.time.LocalDateTime;

public class QcIssueDTO {
    private Long issueId;
    private Long qcItemId;
    private Long auditId;
    private Long elderId;
    private String level;
    private String description;
    private Long responsibleId;
    private String status;
    private Long rectificationId;
    private Long createdBy;
    private LocalDateTime createdAt;

    public static QcIssueDTO from(QcIssue issue) {
        QcIssueDTO dto = new QcIssueDTO();
        dto.setIssueId(issue.getIssueId());
        dto.setQcItemId(issue.getQcItemId());
        dto.setAuditId(issue.getAuditId());
        dto.setElderId(issue.getElderId());
        dto.setLevel(issue.getLevel());
        dto.setDescription(issue.getDescription());
        dto.setResponsibleId(issue.getResponsibleId());
        dto.setStatus(issue.getStatus());
        dto.setRectificationId(issue.getRectificationId());
        dto.setCreatedBy(issue.getCreatedBy());
        dto.setCreatedAt(issue.getCreatedAt());
        return dto;
    }

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public Long getQcItemId() { return qcItemId; }
    public void setQcItemId(Long qcItemId) { this.qcItemId = qcItemId; }
    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getResponsibleId() { return responsibleId; }
    public void setResponsibleId(Long responsibleId) { this.responsibleId = responsibleId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getRectificationId() { return rectificationId; }
    public void setRectificationId(Long rectificationId) { this.rectificationId = rectificationId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
