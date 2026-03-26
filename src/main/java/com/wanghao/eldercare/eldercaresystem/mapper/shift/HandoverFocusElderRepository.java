package com.wanghao.eldercare.eldercaresystem.mapper.shift;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.shift.*;
import com.wanghao.eldercare.eldercaresystem.dto.shift.*;
import com.wanghao.eldercare.eldercaresystem.entity.shift.*;
import com.wanghao.eldercare.eldercaresystem.service.shift.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoverFocusElderRepository extends JpaRepository<HandoverFocusElder, Long> {

    Optional<HandoverFocusElder> findByShiftIdAndElderId(Long shiftId, Long elderId);

    List<HandoverFocusElder> findByShiftIdOrderByCreatedAtDesc(Long shiftId);

    List<HandoverFocusElder> findByShiftIdAndElderIdInOrderByCreatedAtDesc(Long shiftId, List<Long> elderIds);

    long deleteByShiftIdAndElderId(Long shiftId, Long elderId);
}
