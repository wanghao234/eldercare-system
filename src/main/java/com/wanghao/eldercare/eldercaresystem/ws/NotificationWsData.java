package com.wanghao.eldercare.eldercaresystem.ws;

import com.wanghao.eldercare.eldercaresystem.notification.Notification;

import java.time.LocalDateTime;

public class NotificationWsData {
    private Long notificationId;
    private String notifType;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private LocalDateTime createdAt;

    public static NotificationWsData from(Notification n) {
        NotificationWsData data = new NotificationWsData();
        data.setNotificationId(n.getNotificationId());
        data.setNotifType(n.getNotifType());
        data.setTitle(n.getTitle());
        data.setContent(n.getContent());
        data.setBizType(n.getBizType());
        data.setBizId(n.getBizId());
        data.setCreatedAt(n.getCreatedAt());
        return data;
    }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public String getNotifType() { return notifType; }
    public void setNotifType(String notifType) { this.notifType = notifType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
