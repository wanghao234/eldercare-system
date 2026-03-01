package com.wanghao.eldercare.eldercaresystem.care;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BowelRecordRepository extends JpaRepository<BowelRecord, Long> {
    List<BowelRecord> findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(Long elderId, LocalDateTime from, LocalDateTime to);
    List<BowelRecord> findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(List<Long> elderIds, LocalDateTime from, LocalDateTime to);
    List<BowelRecord> findByRecordTimeBetweenOrderByRecordTimeAsc(LocalDateTime from, LocalDateTime to);
    Optional<BowelRecord> findTopByElderIdAndRecordTimeBetweenOrderByRecordTimeDesc(Long elderId, LocalDateTime from, LocalDateTime to);
}
