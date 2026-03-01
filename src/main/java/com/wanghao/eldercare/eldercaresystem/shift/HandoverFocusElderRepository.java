package com.wanghao.eldercare.eldercaresystem.shift;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HandoverFocusElderRepository extends JpaRepository<HandoverFocusElder, Long> {

    Optional<HandoverFocusElder> findByShiftIdAndElderId(Long shiftId, Long elderId);

    List<HandoverFocusElder> findByShiftIdOrderByCreatedAtDesc(Long shiftId);

    List<HandoverFocusElder> findByShiftIdAndElderIdInOrderByCreatedAtDesc(Long shiftId, List<Long> elderIds);

    long deleteByShiftIdAndElderId(Long shiftId, Long elderId);
}
