package com.wanghao.eldercare.eldercaresystem.mapper.shift;

import com.wanghao.eldercare.eldercaresystem.entity.shift.StaffShiftSchedule;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffShiftScheduleRepository extends JpaRepository<StaffShiftSchedule, Long> {

    @Query("""
            select s from StaffShiftSchedule s
            where (:staffId is null or s.staffId = :staffId)
              and (:date is null or s.shiftDate = :date)
              and (:startDate is null or s.shiftDate >= :startDate)
              and (:endDate is null or s.shiftDate <= :endDate)
            order by s.shiftDate asc, s.startTime asc, s.staffId asc, s.shiftId asc
            """)
    List<StaffShiftSchedule> search(@Param("staffId") Long staffId,
                                    @Param("date") LocalDate date,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    @Query("""
            select s from StaffShiftSchedule s
            where (:staffId is null or s.staffId = :staffId)
              and (:shiftType is null or s.shiftType = :shiftType)
              and (:status is null or s.status = :status)
              and (:startDate is null or s.shiftDate >= :startDate)
              and (:endDate is null or s.shiftDate <= :endDate)
            """)
    Page<StaffShiftSchedule> searchPage(@Param("staffId") Long staffId,
                                        @Param("shiftType") String shiftType,
                                        @Param("status") String status,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        Pageable pageable);

    @Query("""
            select s from StaffShiftSchedule s
            where (:staffId is null or s.staffId = :staffId)
              and (:shiftType is null or s.shiftType = :shiftType)
              and (:status is null or s.status = :status)
              and (:date is null or s.shiftDate = :date)
              and (:startDate is null or s.shiftDate >= :startDate)
              and (:endDate is null or s.shiftDate <= :endDate)
            order by s.shiftDate asc, s.startTime asc, s.staffId asc, s.shiftId asc
            """)
    List<StaffShiftSchedule> searchAdvanced(@Param("staffId") Long staffId,
                                            @Param("shiftType") String shiftType,
                                            @Param("status") String status,
                                            @Param("date") LocalDate date,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    @Query("""
            select s from StaffShiftSchedule s
            where s.staffId = :staffId
              and s.shiftDate = :shiftDate
              and s.status = 'active'
              and (:excludeShiftId is null or s.shiftId <> :excludeShiftId)
              and s.startTime < :endTime
              and s.endTime > :startTime
            order by s.startTime asc, s.shiftId asc
            """)
    List<StaffShiftSchedule> findActiveConflicts(@Param("staffId") Long staffId,
                                                 @Param("shiftDate") LocalDate shiftDate,
                                                 @Param("startTime") LocalTime startTime,
                                                 @Param("endTime") LocalTime endTime,
                                                 @Param("excludeShiftId") Long excludeShiftId);

    List<StaffShiftSchedule> findAllByShiftDateBetweenOrderByShiftDateAscStaffIdAscStartTimeAscShiftIdAsc(
            LocalDate startDate,
            LocalDate endDate
    );

    List<StaffShiftSchedule> findAllByShiftDateBetweenAndStatusOrderByShiftDateAscStaffIdAscStartTimeAscShiftIdAsc(
            LocalDate startDate,
            LocalDate endDate,
            String status
    );

    List<StaffShiftSchedule> findAllByStaffIdInAndShiftDateInAndStatusOrderByShiftDateAscStartTimeAsc(
            Collection<Long> staffIds,
            Collection<LocalDate> dates,
            String status
    );
}
