package com.wanghao.eldercare.eldercaresystem.service.careplan;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CarePlanTaskScheduleGenerator {

    private static final int MAX_GENERATE_DAYS = 90;

    public List<ScheduledTaskSlot> expand(TaskScheduleRequest request) {
        if (request == null) {
            return List.of();
        }

        LocalDate startDate = request.startDate() == null ? LocalDate.now() : request.startDate();
        LocalDate endDate = resolveEndDate(startDate, request.endDate());
        if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }

        String frequency = safe(request.frequencyDesc());
        String taskTypeCode = safe(request.taskTypeCode()).toLowerCase(Locale.ROOT);
        List<LocalTime> times = resolveTimes(frequency, safe(request.suggestedTime()), taskTypeCode);

        if (!StringUtils.hasText(frequency) && isDefaultDailyTask(taskTypeCode)) {
            frequency = "每日一次";
        }

        List<LocalDate> dates = resolveDates(startDate, endDate, frequency);
        Set<String> uniqueKeys = new LinkedHashSet<>();
        List<ScheduledTaskSlot> slots = new ArrayList<>();
        for (LocalDate date : dates) {
            for (LocalTime time : times) {
                LocalDateTime scheduledAt = LocalDateTime.of(date, time);
                String key = date + "|" + time;
                if (uniqueKeys.add(key)) {
                    slots.add(new ScheduledTaskSlot(date, time, scheduledAt));
                }
            }
        }
        return slots;
    }

    private LocalDate resolveEndDate(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedEnd = endDate == null ? startDate.plusDays(6) : endDate;
        LocalDate maxEnd = startDate.plusDays(MAX_GENERATE_DAYS - 1L);
        return resolvedEnd.isAfter(maxEnd) ? maxEnd : resolvedEnd;
    }

    private List<LocalDate> resolveDates(LocalDate startDate, LocalDate endDate, String frequency) {
        String normalized = normalizeFrequency(frequency);
        if (normalized.contains("每周两次")) {
            return expandByWeekdays(startDate, endDate, List.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY));
        }
        if (normalized.contains("每周三次")) {
            return expandByWeekdays(startDate, endDate, List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
        }
        if (normalized.contains("每两周一次") || normalized.contains("每2周一次") || normalized.contains("双周一次")) {
            return expandByInterval(startDate, endDate, 14);
        }
        if (normalized.contains("每周一次")) {
            return expandByInterval(startDate, endDate, 7);
        }
        return expandByInterval(startDate, endDate, 1);
    }

    private List<LocalDate> expandByInterval(LocalDate startDate, LocalDate endDate, int intervalDays) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(intervalDays)) {
            dates.add(date);
        }
        return dates;
    }

    private List<LocalDate> expandByWeekdays(LocalDate startDate, LocalDate endDate, List<DayOfWeek> weekdays) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        for (DayOfWeek weekday : weekdays) {
            LocalDate current = startDate.with(TemporalAdjusters.nextOrSame(weekday));
            while (!current.isAfter(endDate)) {
                dates.add(current);
                current = current.plusWeeks(1);
            }
        }
        return dates.stream().sorted().toList();
    }

    private List<LocalTime> resolveTimes(String frequency, String suggestedTime, String taskTypeCode) {
        List<LocalTime> parsedTimes = parseTimes(suggestedTime);
        if (isDailyTwiceFrequency(frequency)) {
            if (parsedTimes.size() >= 2) {
                return parsedTimes.subList(0, 2);
            }
            if (parsedTimes.size() == 1) {
                return List.of(parsedTimes.get(0), LocalTime.of(20, 0));
            }
            return List.of(LocalTime.of(8, 0), LocalTime.of(20, 0));
        }
        if (!parsedTimes.isEmpty()) {
            return List.of(parsedTimes.get(0));
        }
        return List.of(defaultTimeFor(taskTypeCode));
    }

    private List<LocalTime> parseTimes(String suggestedTime) {
        if (!StringUtils.hasText(suggestedTime)) {
            return List.of();
        }
        String normalized = suggestedTime.replace('，', ',')
                .replace('、', ',')
                .replace('/', ',')
                .replace('|', ',');
        String[] parts = normalized.split(",");
        List<LocalTime> times = new ArrayList<>();
        for (String part : parts) {
            String candidate = part == null ? "" : part.trim();
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            LocalTime parsed = tryParseTime(candidate);
            if (parsed != null) {
                times.add(parsed);
            }
        }
        return times;
    }

    private LocalTime tryParseTime(String value) {
        String candidate = value.trim();
        if (candidate.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
            return LocalTime.parse(candidate);
        }
        if (candidate.matches("\\d{1,2}:\\d{2}")) {
            return LocalTime.parse(candidate + ":00");
        }
        return null;
    }

    private boolean isDailyTwiceFrequency(String frequency) {
        String normalized = normalizeFrequency(frequency);
        return normalized.contains("每日两次") || normalized.contains("每天两次") || normalized.contains("一日两次");
    }

    private boolean isDefaultDailyTask(String taskTypeCode) {
        return "daily_care".equals(taskTypeCode)
                || "health_monitoring".equals(taskTypeCode)
                || "medication_care".equals(taskTypeCode);
    }

    private LocalTime defaultTimeFor(String taskTypeCode) {
        return switch (taskTypeCode) {
            case "health_monitoring" -> LocalTime.of(8, 0);
            case "medication_care" -> LocalTime.of(8, 0);
            case "daily_care" -> LocalTime.of(9, 0);
            case "diet_plan" -> LocalTime.of(11, 30);
            case "rehabilitation_activity" -> LocalTime.of(15, 0);
            case "psychological_care" -> LocalTime.of(14, 0);
            case "safety_precaution" -> LocalTime.of(20, 0);
            case "evaluation" -> LocalTime.of(16, 0);
            default -> LocalTime.of(9, 0);
        };
    }

    private String normalizeFrequency(String frequency) {
        return safe(frequency).replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record TaskScheduleRequest(LocalDate startDate,
                                      LocalDate endDate,
                                      String frequencyDesc,
                                      String suggestedTime,
                                      String taskTypeCode) {
    }

    public record ScheduledTaskSlot(LocalDate scheduledDate,
                                    LocalTime scheduledTime,
                                    LocalDateTime scheduledAt) {
    }
}
