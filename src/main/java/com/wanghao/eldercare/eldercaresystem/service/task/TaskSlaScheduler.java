package com.wanghao.eldercare.eldercaresystem.service.task;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.task.*;
import com.wanghao.eldercare.eldercaresystem.dto.task.*;
import com.wanghao.eldercare.eldercaresystem.entity.task.*;
import com.wanghao.eldercare.eldercaresystem.mapper.task.*;
import com.wanghao.eldercare.eldercaresystem.service.audit.AuditService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskSlaScheduler {

    private final TaskRepository taskRepository;
    private final AuditService auditService;

    public TaskSlaScheduler(TaskRepository taskRepository, AuditService auditService) {
        this.taskRepository = taskRepository;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelayString = "${tasks.sla.refresh-interval-ms:60000}")
    public void refreshOverdueTasks() {
        refreshOverdueTasksNow();
    }

    public int refreshOverdueTasksNow() {
        LocalDateTime now = LocalDateTime.now();
        int updated = taskRepository.markOverdue(List.of("pending", "in_progress"), now);
        if (updated > 0) {
            auditService.logSuccess(AuditAction.UPDATE, "tasks", null,
                    Map.of("job", "sla_overdue_refresh", "updatedCount", updated, "refreshedAt", now.toString()));
        }
        return updated;
    }
}
