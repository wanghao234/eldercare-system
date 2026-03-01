package com.wanghao.eldercare.eldercaresystem.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplyIssueRecordRepository extends JpaRepository<SupplyIssueRecord, Long>, JpaSpecificationExecutor<SupplyIssueRecord> {
}
