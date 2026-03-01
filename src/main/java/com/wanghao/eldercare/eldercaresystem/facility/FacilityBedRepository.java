package com.wanghao.eldercare.eldercaresystem.facility;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacilityBedRepository extends JpaRepository<FacilityBed, Long> {
    Page<FacilityBed> findByRoomIdAndStatus(Long roomId, String status, Pageable pageable);

    Page<FacilityBed> findByRoomId(Long roomId, Pageable pageable);

    Page<FacilityBed> findByStatus(String status, Pageable pageable);

    boolean existsByRoomId(Long roomId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update FacilityBed b set b.status=:toStatus where b.bedId=:bedId and b.status=:fromStatus")
    int transitionStatus(@Param("bedId") Long bedId,
                         @Param("fromStatus") String fromStatus,
                         @Param("toStatus") String toStatus);
}
