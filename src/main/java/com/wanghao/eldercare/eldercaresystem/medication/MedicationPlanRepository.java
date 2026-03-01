package com.wanghao.eldercare.eldercaresystem.medication;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MedicationPlanRepository extends JpaRepository<MedicationPlan, Long> {

    List<MedicationPlan> findByElderIdAndStatus(Long elderId, String status);

    List<MedicationPlan> findByElderId(Long elderId);

    Page<MedicationPlan> findByElderId(Long elderId, Pageable pageable);

    Page<MedicationPlan> findByElderIdAndStatus(Long elderId, String status, Pageable pageable);

    Page<MedicationPlan> findByStatus(String status, Pageable pageable);

    Page<MedicationPlan> findByElderIdIn(List<Long> elderIds, Pageable pageable);

    Page<MedicationPlan> findByElderIdInAndStatus(List<Long> elderIds, String status, Pageable pageable);

    boolean existsByMedicationId(Long medicationId);

    @Query("select p.status from MedicationPlan p where p.planId = :planId")
    Optional<String> findStatusByPlanId(@Param("planId") Long planId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update MedicationPlan p set p.status = :toStatus, p.updatedAt = :updatedAt where p.planId = :planId and p.status = :fromStatus")
    int updateStatusIfMatch(@Param("planId") Long planId,
                            @Param("fromStatus") String fromStatus,
                            @Param("toStatus") String toStatus,
                            @Param("updatedAt") LocalDateTime updatedAt);
}
