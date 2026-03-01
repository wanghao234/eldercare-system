package com.wanghao.eldercare.eldercaresystem.task;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final PermissionService permissionService;

    public TaskService(TaskRepository taskRepository, PermissionService permissionService) {
        this.taskRepository = taskRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public TaskListResponse listMyTasks(CurrentUser currentUser, String status, int page, int size) {
        List<String> statuses = parseStatuses(status, List.of("pending", "in_progress", "overdue"));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dueAt"));
        Page<Task> taskPage = taskRepository.findByAssignedToAndStatusIn(currentUser.getUserId(), statuses, pageable);
        return toPage(taskPage, page, size);
    }

    @Transactional(readOnly = true)
    public TaskListResponse listTasks(CurrentUser currentUser,
                                      Long elderId,
                                      Long assignedTo,
                                      String status,
                                      String taskType,
                                      String priority,
                                      String relatedBizType,
                                      Long relatedBizId,
                                      LocalDateTime from,
                                      LocalDateTime to,
                                      int page,
                                      int size) {
        Specification<Task> spec = Specification.where(null);
        if (elderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("elderId"), elderId));
        }
        if (assignedTo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedTo"), assignedTo));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.toLowerCase(Locale.ROOT)));
        }
        if (taskType != null && !taskType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("taskType"), taskType.toLowerCase(Locale.ROOT)));
        }
        if (priority != null && !priority.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority.toLowerCase(Locale.ROOT)));
        }
        if (relatedBizType != null && !relatedBizType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("relatedBizType"), relatedBizType.toLowerCase(Locale.ROOT)));
        }
        if (relatedBizId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("relatedBizId"), relatedBizId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueAt"), to));
        }

        spec = spec.and(buildPermissionSpec(currentUser));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dueAt"));
        Page<Task> result = taskRepository.findAll(spec, pageable);
        return toPage(result, page, size);
    }

    @Transactional
    public TaskListItemDTO createTask(CurrentUser currentUser, CreateTaskRequest request) {
        if (!isAdminOrLeader(currentUser)) {
            throw new AccessDeniedException("仅管理员或护士长可创建任务");
        }
        permissionService.assertCanAccessElder(currentUser, request.getElderId());

        Task task = new Task();
        task.setElderId(request.getElderId());
        task.setTaskType(normalizeOrDefault(request.getTaskType(), "general"));
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(normalizeOrDefault(request.getPriority(), "medium"));
        task.setStatus("pending");
        task.setScheduledAt(request.getScheduledAt());
        task.setDueAt(request.getDueAt());
        task.setAssignedTo(request.getAssignedTo());
        task.setCreatedBy(currentUser.getUserId());
        task.setRelatedBizType(normalizeNullable(request.getRelatedBizType()));
        task.setRelatedBizId(request.getRelatedBizId());
        task.setProcessInstanceId(request.getProcessInstanceId());
        task.setWfTaskId(request.getWfTaskId());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return TaskListItemDTO.from(taskRepository.save(task));
    }

    @Transactional
    public TaskListItemDTO startTask(CurrentUser currentUser, Long taskId) {
        Task task = getTaskOrThrow(taskId);
        assertCanOperateTask(currentUser, task);

        int updated = taskRepository.updateStatusIfMatch(
                taskId,
                List.of("pending", "overdue"),
                "in_progress",
                LocalDateTime.now()
        );
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 pending/overdue -> in_progress，当前状态=" + currentStatus(taskId));
        }
        return TaskListItemDTO.from(getTaskOrThrow(taskId));
    }

    @Transactional
    public TaskListItemDTO completeTask(CurrentUser currentUser, Long taskId) {
        Task task = getTaskOrThrow(taskId);
        assertCanOperateTask(currentUser, task);

        LocalDateTime now = LocalDateTime.now();
        int updated = taskRepository.completeIfMatch(taskId, List.of("in_progress", "overdue"), currentUser.getUserId(), now, now);
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 in_progress/overdue -> completed，当前状态=" + currentStatus(taskId));
        }
        return TaskListItemDTO.from(getTaskOrThrow(taskId));
    }

    @Transactional
    public TaskListItemDTO cancelTask(CurrentUser currentUser, Long taskId) {
        Task task = getTaskOrThrow(taskId);
        assertCanViewTask(currentUser, task);
        if (!isAdminOrLeader(currentUser) && !Objects.equals(task.getCreatedBy(), currentUser.getUserId())) {
            throw new AccessDeniedException("仅任务创建人或管理员/护士长可取消任务");
        }
        if ((currentUser.hasRole("nurse") || currentUser.hasRole("caregiver"))
                && !Objects.equals(task.getAssignedTo(), currentUser.getUserId())) {
            throw new AccessDeniedException("不可取消指派给他人的任务");
        }

        int updated = taskRepository.updateStatusIfMatch(
                taskId,
                List.of("pending", "in_progress", "overdue"),
                "cancelled",
                LocalDateTime.now()
        );
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 pending/in_progress/overdue -> cancelled，当前状态=" + currentStatus(taskId));
        }
        return TaskListItemDTO.from(getTaskOrThrow(taskId));
    }

    @Transactional
    public TaskListItemDTO reassignTask(CurrentUser currentUser, Long taskId, ReassignTaskRequest request) {
        if (!isAdminOrLeader(currentUser)) {
            throw new AccessDeniedException("仅管理员或护士长可改派任务");
        }
        Task task = getTaskOrThrow(taskId);
        task.setAssignedTo(request.getAssignedTo());
        task.setDescription(appendReassignComment(task.getDescription(), currentUser.getUsername(), request.getComment()));
        task.setUpdatedAt(LocalDateTime.now());
        return TaskListItemDTO.from(taskRepository.save(task));
    }

    @Transactional
    public Task createTaskInternal(Task task) {
        task.setStatus(task.getStatus() == null ? "pending" : normalizeOrDefault(task.getStatus(), "pending"));
        task.setTaskType(normalizeOrDefault(task.getTaskType(), "general"));
        task.setPriority(normalizeOrDefault(task.getPriority(), "medium"));
        LocalDateTime now = LocalDateTime.now();
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(now);
        }
        task.setUpdatedAt(now);
        return taskRepository.save(task);
    }

    private Specification<Task> buildPermissionSpec(CurrentUser currentUser) {
        if (isAdminOrLeader(currentUser)) {
            return null;
        }
        if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver") || currentUser.hasRole("family")) {
            List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
            if (visibleElderIds == null) {
                return null;
            }
            if (visibleElderIds.isEmpty()) {
                return (root, query, cb) -> cb.disjunction();
            }
            return (root, query, cb) -> root.get("elderId").in(visibleElderIds);
        }
        if (currentUser.hasRole("elder")) {
            return (root, query, cb) -> cb.equal(root.get("elderId"), currentUser.getUserId());
        }
        throw new AccessDeniedException("当前角色无权限访问任务模块");
    }

    private void assertCanViewTask(CurrentUser currentUser, Task task) {
        if (isAdminOrLeader(currentUser)) {
            return;
        }
        if (currentUser.hasRole("elder")) {
            if (Objects.equals(task.getElderId(), currentUser.getUserId())) {
                return;
            }
            throw new AccessDeniedException("无权访问该任务");
        }
        if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver") || currentUser.hasRole("family")) {
            if (task.getElderId() == null) {
                if ((currentUser.hasRole("nurse") || currentUser.hasRole("caregiver"))
                        && (Objects.equals(task.getAssignedTo(), currentUser.getUserId())
                        || Objects.equals(task.getCreatedBy(), currentUser.getUserId()))) {
                    return;
                }
                throw new AccessDeniedException("无权访问该任务");
            }
            permissionService.assertCanAccessElder(currentUser, task.getElderId());
            return;
        }
        throw new AccessDeniedException("当前角色无权限访问任务模块");
    }

    private void assertCanOperateTask(CurrentUser currentUser, Task task) {
        assertCanViewTask(currentUser, task);
        if (isAdminOrLeader(currentUser)) {
            return;
        }
        if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            if (!Objects.equals(task.getAssignedTo(), currentUser.getUserId())) {
                throw new AccessDeniedException("不可操作指派给他人的任务");
            }
            return;
        }
        throw new AccessDeniedException("当前角色无权限执行任务动作");
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("任务不存在"));
    }

    private String currentStatus(Long taskId) {
        return taskRepository.findStatusByTaskId(taskId).orElse("unknown");
    }

    private boolean isAdminOrLeader(CurrentUser currentUser) {
        return currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader");
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private List<String> parseStatuses(String csv, List<String> defaultStatuses) {
        if (csv == null || csv.isBlank()) {
            return defaultStatuses;
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }

    private TaskListResponse toPage(Page<Task> taskPage, int page, int size) {
        TaskListResponse response = new TaskListResponse();
        response.setContent(taskPage.getContent().stream().map(TaskListItemDTO::from).toList());
        response.setTotalElements(taskPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private String appendReassignComment(String oldDescription, String operator, String comment) {
        String mark = "[reassign@" + LocalDateTime.now() + " by " + operator + "] " + (comment == null ? "" : comment);
        if (oldDescription == null || oldDescription.isBlank()) {
            return mark;
        }
        return oldDescription + "\n" + mark;
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }
}
