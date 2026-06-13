package com.wanghao.eldercare.eldercaresystem.mapper.workflow;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.workflow.*;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.*;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.workflow.*;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WfInstanceRepository extends JpaRepository<WfInstance, Long> {
    Optional<WfInstance> findByBizTypeAndBizId(String bizType, Long bizId);

    Optional<WfInstance> findByExternalInstanceId(String externalInstanceId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update WfInstance i set i.status='completed', i.endedAt=:endedAt where i.instanceId=:instanceId and i.status='running'")
    int completeIfRunning(@Param("instanceId") Long instanceId,
                          @Param("endedAt") LocalDateTime endedAt);
}
