package com.wanghao.eldercare.eldercaresystem.shift;

import jakarta.validation.constraints.NotBlank;

public class CreateHandoverNoteRequest {

    @NotBlank(message = "content 不能为空")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
