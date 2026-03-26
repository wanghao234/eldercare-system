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

public interface FacilityRoomRepository extends JpaRepository<FacilityRoom, Long> {
    Page<FacilityRoom> findByFloorIdAndStatus(Long floorId, String status, Pageable pageable);

    Page<FacilityRoom> findByFloorId(Long floorId, Pageable pageable);

    java.util.List<FacilityRoom> findAllByFloorId(Long floorId);

    java.util.List<FacilityRoom> findAllByFloorIdIn(java.util.Collection<Long> floorIds);

    java.util.List<FacilityRoom> findAllByRoomIdIn(java.util.Collection<Long> roomIds);

    void deleteAllByFloorId(Long floorId);

    void deleteAllByFloorIdIn(java.util.Collection<Long> floorIds);

    Page<FacilityRoom> findByStatus(String status, Pageable pageable);

    boolean existsByFloorId(Long floorId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update FacilityRoom r set r.status=:toStatus where r.roomId=:roomId and r.status=:fromStatus")
    int transitionStatus(@Param("roomId") Long roomId,
                         @Param("fromStatus") String fromStatus,
                         @Param("toStatus") String toStatus);
}
