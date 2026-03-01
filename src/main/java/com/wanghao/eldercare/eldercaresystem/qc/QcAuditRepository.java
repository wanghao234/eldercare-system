package com.wanghao.eldercare.eldercaresystem.qc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QcAuditRepository extends JpaRepository<QcAudit, Long>, JpaSpecificationExecutor<QcAudit> {
}
