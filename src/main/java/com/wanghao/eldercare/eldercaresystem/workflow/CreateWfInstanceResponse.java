package com.wanghao.eldercare.eldercaresystem.workflow;

public class CreateWfInstanceResponse {
    private Long instanceId;

    public CreateWfInstanceResponse() {
    }

    public CreateWfInstanceResponse(Long instanceId) {
        this.instanceId = instanceId;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }
}
