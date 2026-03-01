package com.wanghao.eldercare.eldercaresystem.medication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MedicationAdminRecordRepository extends JpaRepository<MedicationAdminRecord, Long>, JpaSpecificationExecutor<MedicationAdminRecord> {
}
