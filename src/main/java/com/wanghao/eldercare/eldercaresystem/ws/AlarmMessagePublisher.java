package com.wanghao.eldercare.eldercaresystem.ws;

import com.wanghao.eldercare.eldercaresystem.alarm.Alarm;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlarmMessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public AlarmMessagePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishCreated(Alarm alarm) {
        AlarmWsMessage message = new AlarmWsMessage("alarm.created", AlarmWsData.from(alarm));
        messagingTemplate.convertAndSend("/topic/alarms", message);
    }

    public void publishUpdated(Alarm alarm, String targetUsername) {
        AlarmWsMessage message = new AlarmWsMessage("alarm.updated", AlarmWsData.from(alarm));
        messagingTemplate.convertAndSend("/topic/alarms", message);
        if (targetUsername != null && !targetUsername.isBlank()) {
            messagingTemplate.convertAndSendToUser(targetUsername, "/queue/alarms", message);
        }
    }
}
