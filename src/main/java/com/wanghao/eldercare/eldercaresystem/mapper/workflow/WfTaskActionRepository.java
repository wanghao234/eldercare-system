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
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WfTaskActionRepository extends JpaRepository<WfTaskAction, Long> {
    List<WfTaskAction> findByWfTaskIdOrderByActionTimeAsc(Long wfTaskId);

    List<WfTaskAction> findByInstanceIdOrderByActionTimeAsc(Long instanceId);
}
