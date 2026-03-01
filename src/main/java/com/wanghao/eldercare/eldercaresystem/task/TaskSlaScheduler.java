package com.wanghao.eldercare.eldercaresystem.task;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.AuditService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
