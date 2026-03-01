package com.wanghao.eldercare.eldercaresystem.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityParticipantRepository extends JpaRepository<ActivityParticipant, Long> {

    Optional<ActivityParticipant> findByActivityIdAndElderId(Long activityId, Long elderId);

    Page<ActivityParticipant> findByActivityId(Long activityId, Pageable pageable);
}

