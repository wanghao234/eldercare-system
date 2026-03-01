package com.wanghao.eldercare.eldercaresystem.careplan;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CarePlanRepository extends JpaRepository<CarePlan, Long>, JpaSpecificationExecutor<CarePlan> {

    Optional<CarePlan> findByElderIdAndStatus(Long elderId, String status);
    boolean existsByElderIdAndVersion(Long elderId, Integer version);
    boolean existsByElderIdAndVersionAndCarePlanIdNot(Long elderId, Integer version, Long carePlanId);

    @Query("select coalesce(max(c.version),0) from CarePlan c where c.elderId=:elderId")
    Integer findMaxVersionByElderId(@Param("elderId") Long elderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update CarePlan c set c.status='inactive', c.updatedAt=:updatedAt where c.elderId=:elderId and c.status='active'")
    int deactivateActiveByElderId(@Param("elderId") Long elderId,
                                  @Param("updatedAt") LocalDateTime updatedAt);
}
