package com.wanghao.eldercare.eldercaresystem.rectification;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class CreateRectificationActionRequest {

    @NotBlank(message = "actionType 不能为空")
    private String actionType;

    private String content;

    private List<String> attachments;

    private JsonNode extraJson;

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    public JsonNode getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(JsonNode extraJson) {
        this.extraJson = extraJson;
    }
}
