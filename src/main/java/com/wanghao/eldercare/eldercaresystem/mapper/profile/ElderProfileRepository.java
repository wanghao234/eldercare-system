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
import com.wanghao.eldercare.eldercaresystem.entity.profile.ElderProfileEntity;
import com.wanghao.eldercare.eldercaresystem.service.profile.*;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ElderProfileRepository extends JpaRepository<ElderProfileEntity, Long> {

    List<ElderProfileEntity> findByElderIdIn(Collection<Long> elderIds);

    @Query("""
            SELECT e.elderId FROM ElderProfileEntity e
            WHERE LOWER(e.careLevel) = LOWER(:careLevel)
            """)
    List<Long> findElderIdsByCareLevel(@Param("careLevel") String careLevel);

    @Query("""
            SELECT e.elderId FROM ElderProfileEntity e
            WHERE LOWER(e.careLevel) = LOWER(:careLevel)
              AND e.elderId IN :elderIds
            """)
    List<Long> findElderIdsByCareLevelAndElderIds(@Param("careLevel") String careLevel, @Param("elderIds") Collection<Long> elderIds);
}
