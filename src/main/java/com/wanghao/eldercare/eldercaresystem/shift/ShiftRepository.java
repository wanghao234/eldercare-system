package com.wanghao.eldercare.eldercaresystem.shift;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<Shift, Long>, JpaSpecificationExecutor<Shift> {

    boolean existsByShiftDateAndShiftType(java.time.LocalDate shiftDate, String shiftType);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Shift s set s.status=:to where s.shiftId=:id and s.status=:from")
    int transitionStatus(@Param("id") Long id, @Param("from") String from, @Param("to") String to);
}
