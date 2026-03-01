package com.wanghao.eldercare.eldercaresystem.qc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateQcIssueRequest {

    @NotNull(message = "qcItemId 不能为空")
    private Long qcItemId;

    @NotBlank(message = "level 不能为空")
    private String level;

    @NotBlank(message = "description 不能为空")
    private String description;

    @NotNull(message = "responsibleId 不能为空")
    private Long responsibleId;

    public Long getQcItemId() {
        return qcItemId;
    }

    public void setQcItemId(Long qcItemId) {
        this.qcItemId = qcItemId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getResponsibleId() {
        return responsibleId;
    }

    public void setResponsibleId(Long responsibleId) {
        this.responsibleId = responsibleId;
    }
}
