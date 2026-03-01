package com.wanghao.eldercare.eldercaresystem.rectification;

public class RectificationCreateResponse {
    private Long rectificationId;

    public RectificationCreateResponse() {
    }

    public RectificationCreateResponse(Long rectificationId) {
        this.rectificationId = rectificationId;
    }

    public Long getRectificationId() {
        return rectificationId;
    }

    public void setRectificationId(Long rectificationId) {
        this.rectificationId = rectificationId;
    }
}
