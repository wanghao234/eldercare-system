package com.wanghao.eldercare.eldercaresystem.care;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MealIntakeRecordRepository extends JpaRepository<MealIntakeRecord, Long> {
    List<MealIntakeRecord> findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(Long elderId, LocalDateTime from, LocalDateTime to);
    List<MealIntakeRecord> findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(List<Long> elderIds, LocalDateTime from, LocalDateTime to);
    List<MealIntakeRecord> findByRecordTimeBetweenOrderByRecordTimeAsc(LocalDateTime from, LocalDateTime to);
}
