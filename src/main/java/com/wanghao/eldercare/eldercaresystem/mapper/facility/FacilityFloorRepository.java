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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityFloorRepository extends JpaRepository<FacilityFloor, Long> {
    Page<FacilityFloor> findByDeletedAtIsNull(Pageable pageable);

    Page<FacilityFloor> findByBuildingIdAndDeletedAtIsNull(Long buildingId, Pageable pageable);

    java.util.List<FacilityFloor> findAllByBuildingIdAndDeletedAtIsNull(Long buildingId);

    java.util.List<FacilityFloor> findAllByFloorIdInAndDeletedAtIsNull(java.util.Collection<Long> floorIds);

    java.util.Optional<FacilityFloor> findByFloorIdAndDeletedAtIsNull(Long floorId);

    boolean existsByBuildingId(Long buildingId);
}
