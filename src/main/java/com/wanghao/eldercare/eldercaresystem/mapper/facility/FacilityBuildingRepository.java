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

public interface FacilityBuildingRepository extends JpaRepository<FacilityBuilding, Long> {
    Page<FacilityBuilding> findByDeletedAtIsNull(Pageable pageable);

    Page<FacilityBuilding> findByBuildingNameContainingIgnoreCaseAndDeletedAtIsNull(String keyword, Pageable pageable);

    java.util.Optional<FacilityBuilding> findByBuildingIdAndDeletedAtIsNull(Long buildingId);

    java.util.List<FacilityBuilding> findAllByBuildingIdInAndDeletedAtIsNull(java.util.Collection<Long> buildingIds);
}
