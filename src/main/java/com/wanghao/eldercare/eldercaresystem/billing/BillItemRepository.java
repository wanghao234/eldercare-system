package com.wanghao.eldercare.eldercaresystem.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillItemRepository extends JpaRepository<BillItem, Long> {

    boolean existsByFeeItemId(Long feeItemId);

    List<BillItem> findByBillId(Long billId);
}
