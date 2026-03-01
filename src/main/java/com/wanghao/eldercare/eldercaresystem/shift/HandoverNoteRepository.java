package com.wanghao.eldercare.eldercaresystem.shift;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoverNoteRepository extends JpaRepository<HandoverNote, Long> {
    Page<HandoverNote> findByShiftId(Long shiftId, Pageable pageable);
}
