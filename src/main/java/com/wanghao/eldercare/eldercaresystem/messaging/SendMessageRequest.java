package com.wanghao.eldercare.eldercaresystem.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SendMessageRequest {
    private Long elderId;
    @NotNull
    private Long receiverId;
    @NotBlank
    private String contentType;
    @NotBlank
    private String content;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
