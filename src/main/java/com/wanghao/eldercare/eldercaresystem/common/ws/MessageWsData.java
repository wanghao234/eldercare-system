package com.wanghao.eldercare.eldercaresystem.common.ws;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.entity.messaging.Message;
import java.time.LocalDateTime;

public class MessageWsData {
    private Long messageId;
    private Long elderId;
    private Long senderId;
    private Long receiverId;
    private String contentType;
    private String content;
    private LocalDateTime createdAt;

    public static MessageWsData from(Message m) {
        MessageWsData data = new MessageWsData();
        data.setMessageId(m.getMessageId());
        data.setElderId(m.getElderId());
        data.setSenderId(m.getSenderId());
        data.setReceiverId(m.getReceiverId());
        data.setContentType(m.getContentType());
        data.setContent(m.getContent());
        data.setCreatedAt(m.getCreatedAt());
        return data;
    }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
