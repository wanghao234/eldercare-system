package com.wanghao.eldercare.eldercaresystem.mapper.profile;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.profile.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.profile.*;
import com.wanghao.eldercare.eldercaresystem.dto.profile.*;
import com.wanghao.eldercare.eldercaresystem.entity.profile.*;
import com.wanghao.eldercare.eldercaresystem.entity.profile.StaffProfileEntity;
import com.wanghao.eldercare.eldercaresystem.service.profile.*;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffProfileRepository extends JpaRepository<StaffProfileEntity, Long> {
    List<StaffProfileEntity> findByStaffIdIn(Collection<Long> staffIds);
}
