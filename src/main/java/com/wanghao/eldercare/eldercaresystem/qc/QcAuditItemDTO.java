package com.wanghao.eldercare.eldercaresystem.qc;

import java.time.LocalDateTime;

public class QcAuditItemDTO {
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String result;
    private String issues;
    private String evidenceJson;
    private Long checkedBy;
    private LocalDateTime checkedAt;

    public static QcAuditItemDTO from(QcAuditItem item) {
        QcAuditItemDTO dto = new QcAuditItemDTO();
        dto.setItemId(item.getItemId());
        dto.setItemCode(item.getItemCode());
        dto.setItemName(item.getItemName());
        dto.setResult(item.getResult());
        dto.setIssues(item.getIssues());
        dto.setEvidenceJson(item.getEvidenceJson());
        dto.setCheckedBy(item.getCheckedBy());
        dto.setCheckedAt(item.getCheckedAt());
        return dto;
    }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getIssues() { return issues; }
    public void setIssues(String issues) { this.issues = issues; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public Long getCheckedBy() { return checkedBy; }
    public void setCheckedBy(Long checkedBy) { this.checkedBy = checkedBy; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
}
