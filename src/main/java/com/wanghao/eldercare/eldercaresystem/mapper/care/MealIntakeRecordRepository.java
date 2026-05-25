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
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealIntakeRecordRepository extends JpaRepository<MealIntakeRecord, Long> {
    List<MealIntakeRecord> findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(Long elderId, LocalDateTime from, LocalDateTime to);
    List<MealIntakeRecord> findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(List<Long> elderIds, LocalDateTime from, LocalDateTime to);
    List<MealIntakeRecord> findByElderIdOrderByRecordTimeAsc(Long elderId);
    List<MealIntakeRecord> findByElderIdInOrderByRecordTimeAsc(List<Long> elderIds);
    List<MealIntakeRecord> findByRecordTimeBetweenOrderByRecordTimeAsc(LocalDateTime from, LocalDateTime to);
}
