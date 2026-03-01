package com.wanghao.eldercare.eldercaresystem.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class CompleteWfTaskRequest {

    @NotBlank(message = "action 不能为空")
    private String action;

    private String comment;

    private JsonNode formData;

    private List<String> attachments;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public JsonNode getFormData() { return formData; }
    public void setFormData(JsonNode formData) { this.formData = formData; }
    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }
}
