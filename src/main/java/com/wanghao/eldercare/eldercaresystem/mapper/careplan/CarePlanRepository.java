package com.wanghao.eldercare.eldercaresystem.mapper.careplan;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careplan.*;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.service.careplan.*;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
