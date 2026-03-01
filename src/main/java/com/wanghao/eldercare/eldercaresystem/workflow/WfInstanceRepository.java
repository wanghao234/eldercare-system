package com.wanghao.eldercare.eldercaresystem.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WfInstanceRepository extends JpaRepository<WfInstance, Long> {
    Optional<WfInstance> findByBizTypeAndBizId(String bizType, Long bizId);
}
