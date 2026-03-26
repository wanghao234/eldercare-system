package com.wanghao.eldercare.eldercaresystem.service.task;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.task.*;
import com.wanghao.eldercare.eldercaresystem.dto.task.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.Alarm;
import com.wanghao.eldercare.eldercaresystem.entity.rectification.Rectification;
import com.wanghao.eldercare.eldercaresystem.entity.task.*;
import com.wanghao.eldercare.eldercaresystem.entity.visit.VisitRequest;
import com.wanghao.eldercare.eldercaresystem.mapper.task.*;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskIntegrationService {

    private final TaskService taskService;

    public TaskIntegrationService(TaskService taskService) {
        this.taskService = taskService;
    }

    @Transactional
    public void createRectificationExecutionTask(Rectification rectification, Long actorId) {
        if (rectification == null || rectification.getOwnerId() == null) {
            return;
        }

        Task task = new Task();
        task.setElderId(rectification.getOwnerId());
        task.setTaskType("rectification");
        task.setTitle("整改执行：" + nullSafe(rectification.getTitle()));
        task.setDescription(rectification.getDescription());
        task.setPriority(mapLevelToPriority(rectification.getLevel()));
        task.setDueAt(rectification.getDueAt());
        task.setAssignedTo(rectification.getOwnerId());
        task.setCreatedBy(actorId);
        task.setRelatedBizType("rectification");
        task.setRelatedBizId(rectification.getRectificationId());
        taskService.createTaskInternal(task);
    }

    @Transactional
    public void createVisitHandoverTask(VisitRequest visitRequest, Long actorId) {
        if (visitRequest == null) {
            return;
        }
        Task task = new Task();
        task.setElderId(visitRequest.getElderId());
        task.setTaskType("visit_handover");
        task.setTitle("探视外出交接");
        task.setDescription("外出审批通过后的交接准备与归院确认");
        task.setPriority("medium");
        task.setScheduledAt(LocalDateTime.now());
        task.setDueAt(visitRequest.getPlannedStartAt() == null ? LocalDateTime.now().plusHours(1) : visitRequest.getPlannedStartAt());
        task.setAssignedTo(actorId);
        task.setCreatedBy(actorId);
        task.setRelatedBizType("visit");
        task.setRelatedBizId(visitRequest.getRequestId());
        taskService.createTaskInternal(task);
    }

    @Transactional
    public void createAlarmActionTask(Alarm alarm, Long actorId) {
        if (alarm == null) {
            return;
        }
        Task task = new Task();
        task.setElderId(alarm.getElderId());
        task.setTaskType("alarm_action");
        task.setTitle("报警处置跟进");
        task.setDescription("报警接单后处置任务");
        task.setPriority(mapSeverityToPriority(alarm.getSeverity()));
        task.setScheduledAt(LocalDateTime.now());
        task.setDueAt(LocalDateTime.now().plusMinutes(10));
        task.setAssignedTo(actorId);
        task.setCreatedBy(actorId);
        task.setRelatedBizType("alarm");
        task.setRelatedBizId(alarm.getAlarmId());
        task.setProcessInstanceId(alarm.getProcessInstanceId());
        taskService.createTaskInternal(task);
    }

    private String mapLevelToPriority(String level) {
        if (level == null) {
            return "medium";
        }
        return switch (level.toLowerCase(Locale.ROOT)) {
            case "critical" -> "high";
            case "minor" -> "low";
            default -> "medium";
        };
    }

    private String mapSeverityToPriority(String severity) {
        if (severity == null) {
            return "medium";
        }
        return switch (severity.toLowerCase(Locale.ROOT)) {
            case "critical", "high" -> "high";
            case "low" -> "low";
            default -> "medium";
        };
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
