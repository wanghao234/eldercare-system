package com.wanghao.eldercare.eldercaresystem.admission;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface DischargeRecordRepository extends JpaRepository<DischargeRecord, Long>, JpaSpecificationExecutor<DischargeRecord> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update DischargeRecord d set d.status='settling', d.settlementAmount=:settlementAmount, d.refundAmount=:refundAmount, d.updatedAt=:updatedAt where d.dischargeId=:id and d.status='pending'")
    int settlementIfPending(@Param("id") Long id,
                            @Param("settlementAmount") BigDecimal settlementAmount,
                            @Param("refundAmount") BigDecimal refundAmount,
                            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update DischargeRecord d set d.status='completed', d.actualDate=:actualDate, d.updatedAt=:updatedAt where d.dischargeId=:id and d.status in ('settling','approved')")
    int completeIfSettlingOrApproved(@Param("id") Long id,
                                     @Param("actualDate") LocalDate actualDate,
                                     @Param("updatedAt") LocalDateTime updatedAt);
}
