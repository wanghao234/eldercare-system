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
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarePlanChangeRepository extends JpaRepository<CarePlanChangeRequest, Long>, JpaSpecificationExecutor<CarePlanChangeRequest> {

    @Query("""
            select c from CarePlanChangeRequest c
            where c.elderId in :elderIds
            order by c.elderId asc, c.createdAt desc
            """)
    List<CarePlanChangeRequest> findByElderIdInOrderByCreatedAtDesc(@Param("elderIds") Collection<Long> elderIds);

    @Query("""
            select distinct c.elderId from CarePlanChangeRequest c
            where c.status = :status
            order by c.elderId asc
            """)
    List<Long> findDistinctElderIdsByStatus(@Param("status") String status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
            update CarePlanChangeRequest c
               set c.status='approved',
                   c.reviewedBy=:reviewedBy,
                   c.reviewedAt=:reviewedAt,
                   c.reviewComment=:reviewComment,
                   c.updatedAt=:updatedAt
             where c.changeId=:changeId and c.status='pending'
            """)
    int approveIfPending(@Param("changeId") Long changeId,
                         @Param("reviewedBy") Long reviewedBy,
                         @Param("reviewedAt") LocalDateTime reviewedAt,
                         @Param("reviewComment") String reviewComment,
                         @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
            update CarePlanChangeRequest c
               set c.status='rejected',
                   c.reviewedBy=:reviewedBy,
                   c.reviewedAt=:reviewedAt,
                   c.reviewComment=:reviewComment,
                   c.updatedAt=:updatedAt
             where c.changeId=:changeId and c.status='pending'
            """)
    int rejectIfPending(@Param("changeId") Long changeId,
                        @Param("reviewedBy") Long reviewedBy,
                        @Param("reviewedAt") LocalDateTime reviewedAt,
                        @Param("reviewComment") String reviewComment,
                        @Param("updatedAt") LocalDateTime updatedAt);
}
