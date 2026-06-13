package com.wanghao.eldercare.eldercaresystem.service.careplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careplan.*;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CarePlanTaskGenerator {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_TASKS_PER_DAY = 20;
    private static final List<String> UNFINISHED_STATUSES = List.of("pending", "in_progress", "overdue");

    private final TaskRepository taskRepository;
    private final CareTeamAssignmentRepository careTeamAssignmentRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, JsonNode> templates;

    public CarePlanTaskGenerator(TaskRepository taskRepository,
                                 CareTeamAssignmentRepository careTeamAssignmentRepository,
                                 ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.careTeamAssignmentRepository = careTeamAssignmentRepository;
        this.objectMapper = objectMapper;
        this.templates = initTemplates(objectMapper);
    }

    public TaskRegenerationResult regenerate(CarePlan plan, Long operatorId, int days, Long cleanupPlanId) {
        LocalDateTime now = LocalDateTime.now();
        int deleted = 0;
        if (cleanupPlanId != null) {
            deleted += taskRepository.deleteFutureByBizAndStatuses("care_plan", cleanupPlanId, UNFINISHED_STATUSES, now);
        }
        deleted += taskRepository.deleteFutureByBizAndStatuses("care_plan", plan.getCarePlanId(), UNFINISHED_STATUSES, now);

        List<Task> generated = generateTasks(plan, operatorId, sanitizeDays(days), now);
        if (!generated.isEmpty()) {
            taskRepository.saveAll(generated);
        }
        return new TaskRegenerationResult(deleted, generated.size());
    }

    private List<Task> generateTasks(CarePlan plan, Long operatorId, int days, LocalDateTime now) {
        JsonNode root = readJson(plan.getPlanContentJson());
        LocalDate today = now.toLocalDate();
        LocalDate planStartDate = plan.getStartDate();
        LocalDate startDate = maxDate(today, planStartDate == null ? parseDate(root.path("startDate").asText(null)) : planStartDate);
        LocalDate endDate = plan.getEndDate() == null ? parseDate(root.path("endDate").asText(null)) : plan.getEndDate();
        LocalDate rangeEnd = today.plusDays(days - 1L);
        if (endDate != null && endDate.isBefore(rangeEnd)) {
            rangeEnd = endDate;
        }
        if (startDate.isAfter(rangeEnd)) {
            return List.of();
        }

        List<PlanItem> items = resolveItems(root);
        if (items.isEmpty()) {
            return List.of();
        }

        Long assignedNurse = careTeamAssignmentRepository.findActiveNurseIdsByElderId(plan.getElderId())
                .stream()
                .findFirst()
                .orElse(null);

        List<Task> tasks = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(rangeEnd)) {
            int createdToday = 0;
            for (PlanItem item : items) {
                List<LocalTime> dueTimes = buildDueTimes(item);
                for (LocalTime dueTime : dueTimes) {
                    if (createdToday >= MAX_TASKS_PER_DAY) {
                        break;
                    }
                    LocalDateTime dueAt = LocalDateTime.of(cursor, dueTime);
                    Task task = new Task();
                    task.setElderId(plan.getElderId());
                    task.setTaskType(item.type == null ? "care" : item.type);
                    task.setTitle(item.title == null ? "护理任务" : item.title);
                    task.setDescription(item.notes);
                    task.setPriority(item.priority == null ? "medium" : item.priority);
                    task.setStatus("pending");
                    task.setScheduledAt(dueAt);
                    task.setDueAt(dueAt);
                    task.setAssignedTo(assignedNurse);
                    task.setCreatedBy(operatorId);
                    task.setRelatedBizType("care_plan");
                    task.setRelatedBizId(plan.getCarePlanId());
                    task.setCreatedAt(now);
                    task.setUpdatedAt(now);
                    tasks.add(task);
                    createdToday++;
                }
                if (createdToday >= MAX_TASKS_PER_DAY) {
                    break;
                }
            }
            cursor = cursor.plusDays(1);
        }
        return tasks;
    }

    private List<LocalTime> buildDueTimes(PlanItem item) {
        if (!item.times.isEmpty()) {
            return item.times.stream().sorted(Comparator.naturalOrder()).toList();
        }
        if (item.everyMinutes != null && item.everyMinutes > 0) {
            LocalTime start = item.windowStart == null ? LocalTime.MIN : item.windowStart;
            LocalTime end = item.windowEnd == null ? LocalTime.MAX.withSecond(0).withNano(0) : item.windowEnd;
            if (end.isBefore(start)) {
                LocalTime t = start;
                start = end;
                end = t;
            }
            int startMin = start.getHour() * 60 + start.getMinute();
            int endMin = end.getHour() * 60 + end.getMinute();
            int middle = startMin + Math.max(1, (endMin - startMin) / 2);
            Set<Integer> points = new HashSet<>(List.of(startMin, middle, endMin));
            return points.stream()
                    .sorted()
                    .map(min -> LocalTime.of(min / 60, min % 60))
                    .toList();
        }
        return List.of(LocalTime.of(9, 0));
    }

    private List<PlanItem> resolveItems(JsonNode root) {
        List<PlanItem> items = new ArrayList<>();
        String templateKey = trimToNull(root.path("templateKey").asText(null));
        if (templateKey != null) {
            JsonNode template = templates.get(templateKey);
            if (template != null) {
                items.addAll(readItems(template.path("items")));
            }
        }
        items.addAll(readItems(root.path("items")));
        return items;
    }

    private List<PlanItem> readItems(JsonNode itemsNode) {
        if (itemsNode == null || !itemsNode.isArray()) {
            return List.of();
        }
        List<PlanItem> items = new ArrayList<>();
        for (JsonNode node : itemsNode) {
            PlanItem item = new PlanItem();
            item.type = normalize(node.path("type").asText("care"));
            item.title = trimToNull(node.path("title").asText(null));
            item.notes = trimToNull(node.path("notes").asText(null));
            item.priority = normalize(node.path("priority").asText("medium"));
            item.everyMinutes = node.path("everyMinutes").isNumber() ? node.path("everyMinutes").asInt() : null;

            JsonNode timesNode = node.path("times");
            if (timesNode.isArray()) {
                for (JsonNode t : timesNode) {
                    LocalTime parsed = parseTime(t.asText(null));
                    if (parsed != null) {
                        item.times.add(parsed);
                    }
                }
            }
            String window = trimToNull(node.path("timeWindow").asText(null));
            if (window != null && window.contains("-")) {
                String[] seg = window.split("-");
                if (seg.length == 2) {
                    item.windowStart = parseTime(seg[0].trim());
                    item.windowEnd = parseTime(seg[1].trim());
                }
            }
            items.add(item);
        }
        return items;
    }

    private Map<String, JsonNode> initTemplates(ObjectMapper mapper) {
        Map<String, JsonNode> map = new HashMap<>();
        map.put("basic_rounding_v1", readJson("""
                {"templateKey":"basic_rounding_v1","items":[
                  {"type":"rounding","title":"巡房","times":["08:00","14:00","20:00"],"priority":"normal","notes":"观察精神状态"},
                  {"type":"care","title":"翻身护理","everyMinutes":120,"timeWindow":"00:00-23:59","priority":"high","notes":"压疮风险"}
                ]}
                """));
        map.put("night_observe_v1", readJson("""
                {"templateKey":"night_observe_v1","items":[
                  {"type":"rounding","title":"夜间巡查","times":["22:00","02:00","05:00"],"priority":"high","notes":"夜间重点观察"}
                ]}
                """));
        return map;
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private LocalDate parseDate(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalTime parseTime(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return LocalTime.parse(v, HH_MM);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDate maxDate(LocalDate a, LocalDate b) {
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    private int sanitizeDays(int days) {
        if (days <= 0) {
            return 1;
        }
        return Math.min(days, 30);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        String v = trimToNull(value);
        return v == null ? null : v.toLowerCase(Locale.ROOT);
    }

    private static final class PlanItem {
        private String type;
        private String title;
        private String notes;
        private String priority;
        private Integer everyMinutes;
        private LocalTime windowStart;
        private LocalTime windowEnd;
        private final List<LocalTime> times = new ArrayList<>();
    }
}
