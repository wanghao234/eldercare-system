package com.wanghao.eldercare.eldercaresystem.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBillIdOrderByPaidAtDesc(Long billId);
}
