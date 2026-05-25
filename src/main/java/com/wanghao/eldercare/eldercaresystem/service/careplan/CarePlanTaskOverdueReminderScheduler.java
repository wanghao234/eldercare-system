package com.wanghao.eldercare.eldercaresystem.service.careplan;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CarePlanTaskOverdueReminderScheduler {

    private final CarePlanTaskService carePlanTaskService;

    public CarePlanTaskOverdueReminderScheduler(CarePlanTaskService carePlanTaskService) {
        this.carePlanTaskService = carePlanTaskService;
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void remindOverdueTasks() {
        carePlanTaskService.createOverdueNotifications();
    }
}
