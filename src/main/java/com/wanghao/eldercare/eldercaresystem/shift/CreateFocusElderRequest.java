package com.wanghao.eldercare.eldercaresystem.shift;

import jakarta.validation.constraints.NotNull;

public class CreateFocusElderRequest {

    @NotNull(message = "elderId 不能为空")
    private Long elderId;

    private String note;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
