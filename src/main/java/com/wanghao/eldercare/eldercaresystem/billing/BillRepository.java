package com.wanghao.eldercare.eldercaresystem.billing;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Bill b set b.status = :toStatus where b.billId = :billId and b.status = :fromStatus")
    int updateStatusIfMatch(@Param("billId") Long billId,
                            @Param("fromStatus") String fromStatus,
                            @Param("toStatus") String toStatus);
}
