package com.wanghao.eldercare.eldercaresystem.visit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRequestLogRepository extends JpaRepository<VisitRequestLog, Long> {
    List<VisitRequestLog> findByRequestIdOrderByActionTimeAsc(Long requestId);
}
