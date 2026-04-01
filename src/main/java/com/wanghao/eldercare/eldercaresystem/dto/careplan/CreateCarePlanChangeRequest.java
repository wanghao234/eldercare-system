package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.*;
import com.wanghao.eldercare.eldercaresystem.service.careplan.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCarePlanChangeRequest {

    @NotNull(message = "elderId 不能为空")
    private Long elderId;

    private Long draftPlanId;

    @NotBlank(message = "reason 不能为空")
    private String reason;

    private String changeType;
    private Boolean requiresDoctorReview;
    private String proposedTitle;
    private JsonNode proposedContent;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getDraftPlanId() {
        return draftPlanId;
    }

    public void setDraftPlanId(Long draftPlanId) {
        this.draftPlanId = draftPlanId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Boolean getRequiresDoctorReview() {
        return requiresDoctorReview;
    }

    public void setRequiresDoctorReview(Boolean requiresDoctorReview) {
        this.requiresDoctorReview = requiresDoctorReview;
    }

    public String getProposedTitle() {
        return proposedTitle;
    }

    public void setProposedTitle(String proposedTitle) {
        this.proposedTitle = proposedTitle;
    }

    public JsonNode getProposedContent() {
        return proposedContent;
    }

    public void setProposedContent(JsonNode proposedContent) {
        this.proposedContent = proposedContent;
    }
}
