package com.wanghao.eldercare.eldercaresystem.alarm;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long>, JpaSpecificationExecutor<Alarm> {

    @Query("select a.status from Alarm a where a.alarmId = :alarmId")
    Optional<String> findStatusByAlarmId(@Param("alarmId") Long alarmId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Alarm a set a.status = 'accepted', a.acceptedAt = :acceptedAt, a.acceptedBy = :acceptedBy " +
            "where a.alarmId = :alarmId and a.status = 'created'")
    int acceptIfCreated(@Param("alarmId") Long alarmId,
                        @Param("acceptedAt") LocalDateTime acceptedAt,
                        @Param("acceptedBy") Long acceptedBy);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Alarm a set a.status = 'on_site', a.arrivedAt = :arrivedAt, a.arrivedBy = :arrivedBy " +
            "where a.alarmId = :alarmId and a.status = 'accepted'")
    int arriveIfAccepted(@Param("alarmId") Long alarmId,
                        @Param("arrivedAt") LocalDateTime arrivedAt,
                        @Param("arrivedBy") Long arrivedBy);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Alarm a set a.status = 'closed', a.closedAt = :closedAt, a.closedBy = :closedBy, a.closeReason = :closeReason " +
            "where a.alarmId = :alarmId and a.status = 'on_site'")
    int closeIfOnSite(@Param("alarmId") Long alarmId,
                      @Param("closedAt") LocalDateTime closedAt,
                      @Param("closedBy") Long closedBy,
                      @Param("closeReason") String closeReason);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Alarm a set a.status = 'closed', a.closedAt = :closedAt, a.closedBy = :closedBy, a.closeReason = :closeReason " +
            "where a.alarmId = :alarmId and a.status = 'handling'")
    int closeIfHandling(@Param("alarmId") Long alarmId,
                        @Param("closedAt") LocalDateTime closedAt,
                        @Param("closedBy") Long closedBy,
                        @Param("closeReason") String closeReason);
}
