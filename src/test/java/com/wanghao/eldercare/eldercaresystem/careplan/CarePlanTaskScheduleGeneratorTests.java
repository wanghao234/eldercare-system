package com.wanghao.eldercare.eldercaresystem.careplan;

import com.wanghao.eldercare.eldercaresystem.service.careplan.CarePlanTaskScheduleGenerator;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CarePlanTaskScheduleGeneratorTests {

    private final CarePlanTaskScheduleGenerator generator = new CarePlanTaskScheduleGenerator();

    @Test
    void expands_daily_tasks_between_start_and_end_dates() {
        List<CarePlanTaskScheduleGenerator.ScheduledTaskSlot> slots = generator.expand(
                new CarePlanTaskScheduleGenerator.TaskScheduleRequest(
                        LocalDate.of(2026, 5, 7),
                        LocalDate.of(2026, 5, 10),
                        "每日",
                        "08:00",
                        "health_monitoring"));

        assertThat(slots).extracting(slot -> slot.scheduledAt().toString())
                .containsExactly(
                        "2026-05-07T08:00",
                        "2026-05-08T08:00",
                        "2026-05-09T08:00",
                        "2026-05-10T08:00");
    }

    @Test
    void expands_weekly_tasks_every_seven_days() {
        List<CarePlanTaskScheduleGenerator.ScheduledTaskSlot> slots = generator.expand(
                new CarePlanTaskScheduleGenerator.TaskScheduleRequest(
                        LocalDate.of(2026, 5, 7),
                        LocalDate.of(2026, 5, 31),
                        "每周一次",
                        "14:00",
                        "psychological_care"));

        assertThat(slots).extracting(slot -> slot.scheduledAt().toString())
                .containsExactly(
                        "2026-05-07T14:00",
                        "2026-05-14T14:00",
                        "2026-05-21T14:00",
                        "2026-05-28T14:00");
    }

    @Test
    void expands_biweekly_tasks_every_fourteen_days() {
        List<CarePlanTaskScheduleGenerator.ScheduledTaskSlot> slots = generator.expand(
                new CarePlanTaskScheduleGenerator.TaskScheduleRequest(
                        LocalDate.of(2026, 5, 7),
                        LocalDate.of(2026, 5, 31),
                        "每两周一次",
                        "20:00",
                        "safety_precaution"));

        assertThat(slots).extracting(slot -> slot.scheduledAt().toString())
                .containsExactly(
                        "2026-05-07T20:00",
                        "2026-05-21T20:00");
    }
}
