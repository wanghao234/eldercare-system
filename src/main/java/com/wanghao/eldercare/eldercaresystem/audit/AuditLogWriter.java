package com.wanghao.eldercare.eldercaresystem.audit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    public AuditLogWriter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLog log) {
        auditLogRepository.save(log);
    }
}

