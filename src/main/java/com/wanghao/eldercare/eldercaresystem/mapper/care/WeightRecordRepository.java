package com.wanghao.eldercare.eldercaresystem.mapper.care;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.care.*;
import com.wanghao.eldercare.eldercaresystem.dto.care.*;
import com.wanghao.eldercare.eldercaresystem.entity.care.*;
import com.wanghao.eldercare.eldercaresystem.service.care.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    List<WeightRecord> findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(Long elderId, LocalDateTime from, LocalDateTime to);
    List<WeightRecord> findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(List<Long> elderIds, LocalDateTime from, LocalDateTime to);
    List<WeightRecord> findByElderIdOrderByRecordTimeAsc(Long elderId);
    List<WeightRecord> findByElderIdInOrderByRecordTimeAsc(List<Long> elderIds);
    List<WeightRecord> findByRecordTimeBetweenOrderByRecordTimeAsc(LocalDateTime from, LocalDateTime to);
    Optional<WeightRecord> findTopByElderIdOrderByRecordTimeDesc(Long elderId);
    Optional<WeightRecord> findTopByElderIdAndRecordTimeBetweenOrderByRecordTimeDesc(Long elderId, LocalDateTime from, LocalDateTime to);
}
