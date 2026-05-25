package com.wanghao.eldercare.eldercaresystem.mapper.alarm;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.alarm.*;
import com.wanghao.eldercare.eldercaresystem.dto.alarm.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.*;
import com.wanghao.eldercare.eldercaresystem.service.alarm.*;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmRepository extends JpaRepository<Alarm, Long>, JpaSpecificationExecutor<Alarm> {

    Optional<Alarm> findByIdempotencyKey(String idempotencyKey);

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
