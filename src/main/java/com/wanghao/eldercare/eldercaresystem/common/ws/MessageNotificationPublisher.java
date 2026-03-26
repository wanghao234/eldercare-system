package com.wanghao.eldercare.eldercaresystem.common.ws;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.entity.messaging.Message;
import com.wanghao.eldercare.eldercaresystem.entity.notification.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageNotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public MessageNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishMessageNew(String receiverUsername, Message message) {
        if (receiverUsername == null || receiverUsername.isBlank()) {
            return;
        }
        UserWsMessage ws = new UserWsMessage("message.new", MessageWsData.from(message));
        messagingTemplate.convertAndSendToUser(receiverUsername, "/queue/messages", ws);
    }

    public void publishNotificationNew(String receiverUsername, Notification notification) {
        if (receiverUsername == null || receiverUsername.isBlank()) {
            return;
        }
        UserWsMessage ws = new UserWsMessage("notification.new", NotificationWsData.from(notification));
        messagingTemplate.convertAndSendToUser(receiverUsername, "/queue/notifications", ws);
    }
}
