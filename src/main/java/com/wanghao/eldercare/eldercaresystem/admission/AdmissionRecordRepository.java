package com.wanghao.eldercare.eldercaresystem.admission;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface AdmissionRecordRepository extends JpaRepository<AdmissionRecord, Long>, JpaSpecificationExecutor<AdmissionRecord> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update AdmissionRecord a set a.status='ended', a.endDate=:endDate, a.updatedAt=:updatedAt where a.admissionId=:admissionId and a.status <> 'ended'")
    int markEnded(@Param("admissionId") Long admissionId,
                  @Param("endDate") LocalDate endDate,
                  @Param("updatedAt") LocalDateTime updatedAt);
}
