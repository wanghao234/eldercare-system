package com.wanghao.eldercare.eldercaresystem.health;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VitalSignRecordRepository extends JpaRepository<VitalSignRecord, Long> {
    List<VitalSignRecord> findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(Long elderId, LocalDateTime from, LocalDateTime to);
    List<VitalSignRecord> findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(List<Long> elderIds, LocalDateTime from, LocalDateTime to);
    List<VitalSignRecord> findByRecordTimeBetweenOrderByRecordTimeAsc(LocalDateTime from, LocalDateTime to);
    Optional<VitalSignRecord> findTopByElderIdAndRecordTimeBetweenOrderByRecordTimeDesc(Long elderId, LocalDateTime from, LocalDateTime to);
}
