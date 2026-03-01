package com.wanghao.eldercare.eldercaresystem.profile.repo;

import com.wanghao.eldercare.eldercaresystem.profile.entity.StaffProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StaffProfileRepository extends JpaRepository<StaffProfileEntity, Long> {
    List<StaffProfileEntity> findByStaffIdIn(Collection<Long> staffIds);
}
