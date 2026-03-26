package com.wanghao.eldercare.eldercaresystem.common.ws;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.Alarm;
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
