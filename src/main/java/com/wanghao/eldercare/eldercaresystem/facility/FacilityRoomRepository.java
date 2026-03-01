package com.wanghao.eldercare.eldercaresystem.facility;

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

    Page<FacilityRoom> findByStatus(String status, Pageable pageable);

    boolean existsByFloorId(Long floorId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update FacilityRoom r set r.status=:toStatus where r.roomId=:roomId and r.status=:fromStatus")
    int transitionStatus(@Param("roomId") Long roomId,
                         @Param("fromStatus") String fromStatus,
                         @Param("toStatus") String toStatus);
}
