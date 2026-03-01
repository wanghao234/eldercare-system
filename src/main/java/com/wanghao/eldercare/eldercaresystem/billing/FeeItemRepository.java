package com.wanghao.eldercare.eldercaresystem.billing;

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
