package com.wanghao.eldercare.eldercaresystem.careplan;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCarePlanChangeRequest {

    @NotNull(message = "elderId 不能为空")
    private Long elderId;

    @NotBlank(message = "reason 不能为空")
    private String reason;

    private String proposedTitle;
    private JsonNode proposedContent;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
