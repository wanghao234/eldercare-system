package com.wanghao.eldercare.eldercaresystem.mapper.billing;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.billing.*;
import com.wanghao.eldercare.eldercaresystem.dto.billing.*;
import com.wanghao.eldercare.eldercaresystem.entity.billing.*;
import com.wanghao.eldercare.eldercaresystem.service.billing.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeeItemRepository extends JpaRepository<FeeItem, Long> {

    @Query("""
            select f from FeeItem f
            where (:keyword is null or lower(f.itemName) like lower(concat('%', :keyword, '%'))
                   or lower(f.category) like lower(concat('%', :keyword, '%')))
            """)
    Page<FeeItem> search(@Param("keyword") String keyword, Pageable pageable);
}
