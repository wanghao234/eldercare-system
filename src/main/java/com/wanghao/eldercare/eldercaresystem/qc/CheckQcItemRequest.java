package com.wanghao.eldercare.eldercaresystem.qc;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public class CheckQcItemRequest {

    @NotBlank(message = "result 不能为空")
    private String result;

    private String issues;
    private JsonNode evidenceJson;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getIssues() {
        return issues;
    }

    public void setIssues(String issues) {
        this.issues = issues;
    }

    public JsonNode getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(JsonNode evidenceJson) {
        this.evidenceJson = evidenceJson;
    }
}
