package com.wanghao.eldercare.eldercaresystem.facility;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityFloorRepository extends JpaRepository<FacilityFloor, Long> {
    Page<FacilityFloor> findByBuildingId(Long buildingId, Pageable pageable);

    boolean existsByBuildingId(Long buildingId);
}
