package com.wanghao.eldercare.eldercaresystem.mapper.inventory;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.inventory.*;
import com.wanghao.eldercare.eldercaresystem.dto.inventory.*;
import com.wanghao.eldercare.eldercaresystem.entity.inventory.*;
import com.wanghao.eldercare.eldercaresystem.service.inventory.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplyIssueRecordRepository extends JpaRepository<SupplyIssueRecord, Long>, JpaSpecificationExecutor<SupplyIssueRecord> {
}
