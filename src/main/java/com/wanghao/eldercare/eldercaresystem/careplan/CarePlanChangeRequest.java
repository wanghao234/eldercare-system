package com.wanghao.eldercare.eldercaresystem.careplan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "care_plan_change_requests")
public class CarePlanChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "change_id")
    private Long changeId;

    @Column(name = "elder_id", nullable = false)
    private Long elderId;

    @Column(name = "from_care_plan_id")
    private Long fromCarePlanId;

    @Column(name = "change_type", nullable = false, length = 32)
    private String changeType;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "proposed_json", columnDefinition = "TEXT", nullable = false)
    private String proposedJson;

    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", length = 255)
    private String reviewComment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getChangeId() {
        return changeId;
    }

    public void setChangeId(Long changeId) {
        this.changeId = changeId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getCurrentPlanId() {
        return fromCarePlanId;
    }

    public void setCurrentPlanId(Long currentPlanId) {
        this.fromCarePlanId = currentPlanId;
    }

    public Long getFromCarePlanId() {
        return fromCarePlanId;
    }

    public void setFromCarePlanId(Long fromCarePlanId) {
        this.fromCarePlanId = fromCarePlanId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Long getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Long requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getProposedTitle() {
        return null;
    }

    public void setProposedTitle(String proposedTitle) {
        // no-op: proposed title is now carried in proposed_json.
    }

    public String getProposedContentJson() {
        return proposedJson;
    }

    public void setProposedContentJson(String proposedContentJson) {
        this.proposedJson = proposedContentJson;
    }

    public Long getApprovedBy() {
        return reviewedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.reviewedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return reviewedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.reviewedAt = approvedAt;
    }

    public Long getRejectedBy() {
        return reviewedBy;
    }

    public void setRejectedBy(Long rejectedBy) {
        this.reviewedBy = rejectedBy;
    }

    public LocalDateTime getRejectedAt() {
        return reviewedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.reviewedAt = rejectedAt;
    }

    public String getRejectReason() {
        return reviewComment;
    }

    public void setRejectReason(String rejectReason) {
        this.reviewComment = rejectReason;
    }

    public String getProposedJson() {
        return proposedJson;
    }

    public void setProposedJson(String proposedJson) {
        this.proposedJson = proposedJson;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
