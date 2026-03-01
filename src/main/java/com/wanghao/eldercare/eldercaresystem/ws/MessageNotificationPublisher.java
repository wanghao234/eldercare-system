package com.wanghao.eldercare.eldercaresystem.ws;

import com.wanghao.eldercare.eldercaresystem.messaging.Message;
import com.wanghao.eldercare.eldercaresystem.notification.Notification;
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
