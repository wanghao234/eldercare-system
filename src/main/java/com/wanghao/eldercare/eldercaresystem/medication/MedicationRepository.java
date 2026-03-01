package com.wanghao.eldercare.eldercaresystem.medication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    @Query("select m from Medication m where (:keyword is null or lower(m.medicationName) like lower(concat('%', :keyword, '%')))")
    Page<Medication> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
