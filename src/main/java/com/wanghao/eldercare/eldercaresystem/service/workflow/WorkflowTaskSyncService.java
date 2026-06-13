package com.wanghao.eldercare.eldercaresystem.service.workflow;

import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfInstance;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfTask;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfInstanceRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfTaskRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLink;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WorkflowTaskSyncService {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final WfInstanceRepository wfInstanceRepository;
    private final WfTaskRepository wfTaskRepository;
    private final WorkflowActionLogService workflowActionLogService;

    public WorkflowTaskSyncService(TaskService taskService,
                                   RuntimeService runtimeService,
                                   WfInstanceRepository wfInstanceRepository,
                                   WfTaskRepository wfTaskRepository,
                                   WorkflowActionLogService workflowActionLogService) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.wfInstanceRepository = wfInstanceRepository;
        this.wfTaskRepository = wfTaskRepository;
        this.workflowActionLogService = workflowActionLogService;
    }

    @Transactional
    public void syncOpenTasks(WfInstance instance, Long actorId) {
        if (!"flowable".equalsIgnoreCase(instance.getEngineType())
                || !StringUtils.hasText(instance.getExternalInstanceId())) {
            return;
        }
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery()
                .processInstanceId(instance.getExternalInstanceId())
                .list();
        LocalDateTime now = LocalDateTime.now();
        for (org.flowable.task.api.Task flowableTask : tasks) {
            syncTask(instance, flowableTask, actorId, now);
        }
        if (tasks.isEmpty() && isProcessEnded(instance.getExternalInstanceId())) {
            instance.setStatus("completed");
            instance.setEndedAt(now);
            wfInstanceRepository.save(instance);
        }
    }

    @Transactional
    public WfTask syncTask(WfInstance instance, org.flowable.task.api.Task flowableTask, Long actorId, LocalDateTime now) {
        WfTask task = wfTaskRepository.findByExternalTaskId(flowableTask.getId()).orElseGet(WfTask::new);
        boolean created = task.getWfTaskId() == null;
        task.setInstanceId(instance.getInstanceId());
        task.setExternalTaskId(flowableTask.getId());
        task.setNodeKey(flowableTask.getTaskDefinitionKey());
        task.setTaskName(flowableTask.getName());
        task.setAssigneeId(parseAssignee(flowableTask.getAssignee()));
        task.setCandidateRole(resolveCandidateGroup(flowableTask.getId()));
        task.setStatus(task.getAssigneeId() == null ? "pending" : "claimed");
        task.setDueAt(toLocalDateTime(flowableTask.getDueDate()));
        task.setPriority(flowableTask.getPriority());
        if (created) {
            task.setCreatedAt(toLocalDateTime(flowableTask.getCreateTime()));
            if (task.getCreatedAt() == null) {
                task.setCreatedAt(now);
            }
        }
        if (task.getAssigneeId() != null && task.getClaimedAt() == null) {
            task.setClaimedAt(now);
        }
        WfTask saved = wfTaskRepository.save(task);
        if (created) {
            workflowActionLogService.log(saved, "create", actorId, "Flowable 创建用户任务", null, null, now);
        }
        return saved;
    }

    @Transactional
    public void markCompleted(WfTask task,
                              String comment,
                              String formDataJson,
                              String attachmentsJson,
                              LocalDateTime now) {
        task.setStatus("completed");
        task.setCompletedAt(now);
        task.setComment(comment);
        task.setFormDataJson(formDataJson);
        task.setAttachmentsJson(attachmentsJson);
        wfTaskRepository.save(task);
    }

    private boolean isProcessEnded(String processInstanceId) {
        try {
            return runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult() == null;
        } catch (FlowableObjectNotFoundException ex) {
            return true;
        }
    }

    private String resolveCandidateGroup(String taskId) {
        Collection<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        return identityLinks.stream()
                .filter(link -> "candidate".equalsIgnoreCase(link.getType()))
                .map(IdentityLink::getGroupId)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private Long parseAssignee(String assignee) {
        if (!StringUtils.hasText(assignee)) {
            return null;
        }
        try {
            return Long.parseLong(assignee);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
