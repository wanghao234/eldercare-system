package com.wanghao.eldercare.eldercaresystem.alarm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmActionLogRepository extends JpaRepository<AlarmActionLog, Long> {
    List<AlarmActionLog> findByAlarmIdOrderByActionTimeAsc(Long alarmId);
}
