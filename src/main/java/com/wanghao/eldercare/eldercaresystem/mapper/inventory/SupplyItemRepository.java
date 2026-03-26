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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplyItemRepository extends JpaRepository<SupplyItem, Long> {

    @Query("""
            select i from SupplyItem i
            where (:keyword is null
                   or lower(i.itemName) like lower(concat('%', :keyword, '%'))
                   or lower(i.category) like lower(concat('%', :keyword, '%')))
            """)
    Page<SupplyItem> search(@Param("keyword") String keyword, Pageable pageable);
}
