package com.wanghao.eldercare.eldercaresystem.mapper.visit;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.visit.*;
import com.wanghao.eldercare.eldercaresystem.dto.visit.*;
import com.wanghao.eldercare.eldercaresystem.entity.visit.*;
import com.wanghao.eldercare.eldercaresystem.service.visit.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRequestLogRepository extends JpaRepository<VisitRequestLog, Long> {
    List<VisitRequestLog> findByRequestIdOrderByActionTimeAsc(Long requestId);
}
