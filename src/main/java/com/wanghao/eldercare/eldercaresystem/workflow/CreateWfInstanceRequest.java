package com.wanghao.eldercare.eldercaresystem.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateWfInstanceRequest {

    @NotBlank(message = "processKey 不能为空")
    private String processKey;

    @NotBlank(message = "bizType 不能为空")
    private String bizType;

    @NotNull(message = "bizId 不能为空")
    private Long bizId;

    public String getProcessKey() { return processKey; }
    public void setProcessKey(String processKey) { this.processKey = processKey; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
}
