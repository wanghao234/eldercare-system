package com.wanghao.eldercare.eldercaresystem.qc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QcAuditItemRepository extends JpaRepository<QcAuditItem, Long> {
    List<QcAuditItem> findByAuditIdOrderByItemIdAsc(Long auditId);
}
