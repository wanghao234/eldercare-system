package com.wanghao.eldercare.eldercaresystem.dto.messaging;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.messaging.*;
import com.wanghao.eldercare.eldercaresystem.entity.messaging.*;
import com.wanghao.eldercare.eldercaresystem.mapper.messaging.*;
import com.wanghao.eldercare.eldercaresystem.service.messaging.*;
import java.time.LocalDateTime;

public class MessageDTO {
    private Long messageId;
    private Long elderId;
    private Long senderId;
    private Long receiverId;
    private String contentType;
    private String content;
    private Integer isRead;
    private LocalDateTime createdAt;

    public static MessageDTO from(Message m) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(m.getMessageId());
        dto.setElderId(m.getElderId());
        dto.setSenderId(m.getSenderId());
        dto.setReceiverId(m.getReceiverId());
        dto.setContentType(m.getContentType());
        dto.setContent(m.getContent());
        dto.setIsRead(m.getIsRead());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
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
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
