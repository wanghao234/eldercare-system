package com.wanghao.eldercare.eldercaresystem.mapper.health;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.health.*;
import com.wanghao.eldercare.eldercaresystem.dto.health.*;
import com.wanghao.eldercare.eldercaresystem.entity.health.*;
import com.wanghao.eldercare.eldercaresystem.service.health.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VitalSignRecordRepository extends JpaRepository<VitalSignRecord, Long> {
    List<VitalSignRecord> findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(Long elderId, LocalDateTime from, LocalDateTime to);
    List<VitalSignRecord> findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(List<Long> elderIds, LocalDateTime from, LocalDateTime to);
    List<VitalSignRecord> findByRecordTimeBetweenOrderByRecordTimeAsc(LocalDateTime from, LocalDateTime to);
    Optional<VitalSignRecord> findTopByElderIdAndRecordTimeBetweenOrderByRecordTimeDesc(Long elderId, LocalDateTime from, LocalDateTime to);
}
