package com.wanghao.eldercare.eldercaresystem.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WfTaskActionRepository extends JpaRepository<WfTaskAction, Long> {
    List<WfTaskAction> findByWfTaskIdOrderByActionTimeAsc(Long wfTaskId);
}
