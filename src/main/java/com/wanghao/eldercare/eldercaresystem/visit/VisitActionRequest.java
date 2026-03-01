package com.wanghao.eldercare.eldercaresystem.visit;

import com.fasterxml.jackson.databind.JsonNode;

public class VisitActionRequest {
    private String comment;
    private String reason;
    private JsonNode extraJson;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public JsonNode getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(JsonNode extraJson) {
        this.extraJson = extraJson;
    }
}
