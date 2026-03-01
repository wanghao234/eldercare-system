package com.wanghao.eldercare.eldercaresystem.visit;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface VisitRequestRepository extends JpaRepository<VisitRequest, Long>, JpaSpecificationExecutor<VisitRequest> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update VisitRequest v set v.status='confirmed', v.confirmedAt=:time, v.confirmedBy=:actorId, v.updatedAt=:time where v.requestId=:id and v.status='pending'")
    int confirmIfPending(@Param("id") Long id,
                         @Param("time") LocalDateTime time,
                         @Param("actorId") Long actorId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update VisitRequest v set v.status='approved', v.approvedAt=:time, v.approvedBy=:actorId, v.updatedAt=:time where v.requestId=:id and v.status='confirmed'")
    int approveIfConfirmed(@Param("id") Long id,
                           @Param("time") LocalDateTime time,
                           @Param("actorId") Long actorId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update VisitRequest v set v.status='rejected', v.rejectedAt=:time, v.rejectedBy=:actorId, v.rejectReason=:reason, v.updatedAt=:time where v.requestId=:id and v.status in ('pending','confirmed')")
    int rejectIfPendingOrConfirmed(@Param("id") Long id,
                                   @Param("time") LocalDateTime time,
                                   @Param("actorId") Long actorId,
                                   @Param("reason") String reason);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update VisitRequest v set v.status='in_progress', v.checkOutAt=:time, v.checkOutBy=:actorId, v.updatedAt=:time where v.requestId=:id and v.status='approved'")
    int checkOutIfApproved(@Param("id") Long id,
                           @Param("time") LocalDateTime time,
                           @Param("actorId") Long actorId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update VisitRequest v set v.status='completed', v.checkInAt=:time, v.checkInBy=:actorId, v.updatedAt=:time where v.requestId=:id and v.status='in_progress'")
    int checkInIfInProgress(@Param("id") Long id,
                            @Param("time") LocalDateTime time,
                            @Param("actorId") Long actorId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update VisitRequest v set v.status='cancelled', v.cancelledAt=:time, v.cancelledBy=:actorId, v.cancelReason=:reason, v.updatedAt=:time where v.requestId=:id and v.status in ('pending','confirmed','approved')")
    int cancelIfPendingConfirmedApproved(@Param("id") Long id,
                                         @Param("time") LocalDateTime time,
                                         @Param("actorId") Long actorId,
                                         @Param("reason") String reason);
}
