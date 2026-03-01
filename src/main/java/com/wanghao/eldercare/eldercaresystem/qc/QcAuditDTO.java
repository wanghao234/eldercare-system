package com.wanghao.eldercare.eldercaresystem.qc;

import java.time.LocalDateTime;
import java.util.List;

public class QcAuditDTO {
    private Long auditId;
    private Long elderId;
    private String title;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private List<QcAuditItemDTO> items;

    public static QcAuditDTO from(QcAudit audit) {
        QcAuditDTO dto = new QcAuditDTO();
        dto.setAuditId(audit.getAuditId());
        dto.setElderId(audit.getElderId());
        dto.setTitle(audit.getTitle());
        dto.setStatus(audit.getStatus());
        dto.setCreatedBy(audit.getCreatedBy());
        dto.setCreatedAt(audit.getCreatedAt());
        return dto;
    }

    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<QcAuditItemDTO> getItems() { return items; }
    public void setItems(List<QcAuditItemDTO> items) { this.items = items; }
}
