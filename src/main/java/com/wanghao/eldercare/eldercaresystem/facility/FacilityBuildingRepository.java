package com.wanghao.eldercare.eldercaresystem.facility;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityBuildingRepository extends JpaRepository<FacilityBuilding, Long> {
    Page<FacilityBuilding> findByBuildingNameContainingIgnoreCase(String keyword, Pageable pageable);
}
