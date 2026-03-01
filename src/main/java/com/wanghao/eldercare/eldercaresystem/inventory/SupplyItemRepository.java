package com.wanghao.eldercare.eldercaresystem.inventory;

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
