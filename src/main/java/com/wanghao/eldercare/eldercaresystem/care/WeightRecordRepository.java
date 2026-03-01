package com.wanghao.eldercare.eldercaresystem.care;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    List<WeightRecord> findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(Long elderId, LocalDateTime from, LocalDateTime to);
    List<WeightRecord> findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(List<Long> elderIds, LocalDateTime from, LocalDateTime to);
    List<WeightRecord> findByRecordTimeBetweenOrderByRecordTimeAsc(LocalDateTime from, LocalDateTime to);
    Optional<WeightRecord> findTopByElderIdOrderByRecordTimeDesc(Long elderId);
    Optional<WeightRecord> findTopByElderIdAndRecordTimeBetweenOrderByRecordTimeDesc(Long elderId, LocalDateTime from, LocalDateTime to);
}
