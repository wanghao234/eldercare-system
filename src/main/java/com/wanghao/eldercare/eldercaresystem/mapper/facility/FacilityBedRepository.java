package com.wanghao.eldercare.eldercaresystem.mapper.facility;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.facility.*;
import com.wanghao.eldercare.eldercaresystem.dto.facility.*;
import com.wanghao.eldercare.eldercaresystem.entity.facility.*;
import com.wanghao.eldercare.eldercaresystem.service.facility.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacilityBedRepository extends JpaRepository<FacilityBed, Long> {
    Page<FacilityBed> findByRoomIdAndStatusAndDeletedAtIsNull(Long roomId, String status, Pageable pageable);

    Page<FacilityBed> findByRoomIdAndDeletedAtIsNull(Long roomId, Pageable pageable);

    java.util.List<FacilityBed> findAllByRoomIdAndDeletedAtIsNull(Long roomId);

    java.util.List<FacilityBed> findAllByRoomIdInAndDeletedAtIsNull(java.util.Collection<Long> roomIds);

    java.util.List<FacilityBed> findAllByBedIdInAndDeletedAtIsNull(java.util.Collection<Long> bedIds);

    void deleteAllByRoomIdIn(java.util.Collection<Long> roomIds);

    Page<FacilityBed> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    Page<FacilityBed> findByDeletedAtIsNull(Pageable pageable);

    boolean existsByRoomId(Long roomId);

    boolean existsByRoomIdAndStatusAndDeletedAtIsNull(Long roomId, String status);

    boolean existsByRoomIdInAndStatusNotAndDeletedAtIsNull(java.util.Collection<Long> roomIds, String status);

    java.util.Optional<FacilityBed> findByBedIdAndDeletedAtIsNull(Long bedId);

    @Query("""
            select count(b) > 0
              from FacilityBed b
             where b.deletedAt is null
               and lower(b.bedCode) = lower(:bedCode)
            """)
    boolean existsActiveByBedCodeIgnoreCase(@Param("bedCode") String bedCode);

    @Query("""
            select count(b) > 0
              from FacilityBed b
             where b.deletedAt is null
               and lower(b.bedCode) = lower(:bedCode)
               and b.bedId <> :excludeBedId
            """)
    boolean existsActiveByBedCodeIgnoreCaseAndBedIdNot(@Param("bedCode") String bedCode,
                                                       @Param("excludeBedId") Long excludeBedId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
            update FacilityBed b
               set b.status=:toStatus
             where b.bedId=:bedId
               and b.status=:fromStatus
               and b.deletedAt is null
            """)
    int transitionStatus(@Param("bedId") Long bedId,
                         @Param("fromStatus") String fromStatus,
                         @Param("toStatus") String toStatus);
}
