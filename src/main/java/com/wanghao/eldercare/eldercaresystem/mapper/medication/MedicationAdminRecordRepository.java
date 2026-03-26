package com.wanghao.eldercare.eldercaresystem.mapper.medication;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.medication.*;
import com.wanghao.eldercare.eldercaresystem.dto.medication.*;
import com.wanghao.eldercare.eldercaresystem.entity.medication.*;
import com.wanghao.eldercare.eldercaresystem.service.medication.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MedicationAdminRecordRepository extends JpaRepository<MedicationAdminRecord, Long>, JpaSpecificationExecutor<MedicationAdminRecord> {
}
