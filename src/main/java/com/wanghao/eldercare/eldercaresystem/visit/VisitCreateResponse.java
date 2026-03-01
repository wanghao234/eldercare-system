package com.wanghao.eldercare.eldercaresystem.visit;

public class VisitCreateResponse {
    private Long requestId;

    public VisitCreateResponse() {
    }

    public VisitCreateResponse(Long requestId) {
        this.requestId = requestId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
}
