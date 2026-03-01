package com.wanghao.eldercare.eldercaresystem.care;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FluidIntakeRecordRepository extends JpaRepository<FluidIntakeRecord, Long> {
    List<FluidIntakeRecord> findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(Long elderId, LocalDateTime from, LocalDateTime to);
    List<FluidIntakeRecord> findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(List<Long> elderIds, LocalDateTime from, LocalDateTime to);
    List<FluidIntakeRecord> findByRecordTimeBetweenOrderByRecordTimeAsc(LocalDateTime from, LocalDateTime to);
}
