package com.wanghao.eldercare.eldercaresystem.mapper.dict;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.dict.*;
import com.wanghao.eldercare.eldercaresystem.entity.dict.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictTypeRepository extends JpaRepository<DictType, String> {
    List<DictType> findAllByIsActiveOrderByTypeCode(Integer isActive);
}
