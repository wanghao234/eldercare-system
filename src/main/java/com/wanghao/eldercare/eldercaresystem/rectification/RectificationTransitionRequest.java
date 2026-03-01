package com.wanghao.eldercare.eldercaresystem.rectification;

import jakarta.validation.constraints.NotBlank;

public class RectificationTransitionRequest {

    @NotBlank(message = "from 不能为空")
    private String from;

    @NotBlank(message = "to 不能为空")
    private String to;

    private String comment;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
