package com.wanghao.eldercare.eldercaresystem.mapper.admission;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.admission.*;
import com.wanghao.eldercare.eldercaresystem.dto.admission.*;
import com.wanghao.eldercare.eldercaresystem.entity.admission.*;
import com.wanghao.eldercare.eldercaresystem.service.admission.*;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdmissionRecordRepository extends JpaRepository<AdmissionRecord, Long>, JpaSpecificationExecutor<AdmissionRecord> {

    java.util.List<AdmissionRecord> findByElderIdInAndStatusOrderByCreatedAtDescAdmissionIdDesc(java.util.Collection<Long> elderIds,
                                                                                                  String status);
    java.util.Optional<AdmissionRecord> findFirstByElderIdAndStatusOrderByUpdatedAtDescAdmissionIdDesc(Long elderId,
                                                                                                        String status);
    java.util.Optional<AdmissionRecord> findFirstByElderIdAndStatusInOrderByUpdatedAtDescAdmissionIdDesc(Long elderId,
                                                                                                          java.util.Collection<String> statuses);

    boolean existsByElderIdAndEndDateIsNull(Long elderId);
    boolean existsByBedIdAndEndDateIsNull(Long bedId);
    boolean existsByElderIdAndStatus(Long elderId, String status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update AdmissionRecord a set a.status='ended', a.endDate=:endDate, a.updatedAt=:updatedAt where a.admissionId=:admissionId and a.status <> 'ended'")
    int markEnded(@Param("admissionId") Long admissionId,
                  @Param("endDate") LocalDate endDate,
                  @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Audited(action = AuditAction.TRANSITION, entityType = "admission_records", entityIdArg = "admissionId", fromValue = "pending", toValue = "active")
    @Query("update AdmissionRecord a set a.status='active', a.updatedAt=:updatedAt where a.admissionId=:admissionId and a.status='pending'")
    int activateIfPending(@Param("admissionId") Long admissionId,
                          @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
            update AdmissionRecord a
            set a.depositAmount=:depositAmount, a.contractNo=:contractNo, a.packageName=:packageName, a.updatedAt=:updatedAt
            where a.admissionId=:admissionId
            """)
    int updateContractAndDeposit(@Param("admissionId") Long admissionId,
                                 @Param("depositAmount") BigDecimal depositAmount,
                                 @Param("contractNo") String contractNo,
                                 @Param("packageName") String packageName,
                                 @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update AdmissionRecord a set a.processInstanceId=:processInstanceId, a.updatedAt=:updatedAt where a.admissionId=:admissionId")
    int bindProcessInstance(@Param("admissionId") Long admissionId,
                            @Param("processInstanceId") Long processInstanceId,
                            @Param("updatedAt") LocalDateTime updatedAt);
}
