package com.wanghao.eldercare.eldercaresystem.mapper.alarm;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.alarm.*;
import com.wanghao.eldercare.eldercaresystem.dto.alarm.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.*;
import com.wanghao.eldercare.eldercaresystem.service.alarm.*;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmActionLogRepository extends JpaRepository<AlarmActionLog, Long> {
    List<AlarmActionLog> findByAlarmIdOrderByActionTimeAsc(Long alarmId);

    List<AlarmActionLog> findByAlarmIdInOrderByActionTimeDescLogIdDesc(Collection<Long> alarmIds);
}
