package com.wanghao.eldercare.eldercaresystem.mapper.admission;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.admission.*;
import com.wanghao.eldercare.eldercaresystem.dto.admission.*;
import com.wanghao.eldercare.eldercaresystem.entity.admission.*;
import com.wanghao.eldercare.eldercaresystem.service.admission.*;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BedRepository extends JpaRepository<Bed, Long> {

    Optional<Bed> findByBedNoIgnoreCase(String bedNo);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Bed b set b.status='occupied' where b.bedId=:bedId and b.status='available'")
    int occupyIfAvailable(@Param("bedId") Long bedId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Audited(action = AuditAction.TRANSITION, entityType = "beds", entityIdArg = "bedId", fromValue = "available", toValue = "reserved")
    @Query("update Bed b set b.status='reserved' where b.bedId=:bedId and b.status='available'")
    int reserveIfAvailable(@Param("bedId") Long bedId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Audited(action = AuditAction.TRANSITION, entityType = "beds", entityIdArg = "bedId", fromValue = "reserved", toValue = "occupied")
    @Query("update Bed b set b.status='occupied' where b.bedId=:bedId and b.status='reserved'")
    int occupyIfReserved(@Param("bedId") Long bedId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Audited(action = AuditAction.TRANSITION, entityType = "beds", entityIdArg = "bedId", fromValue = "reserved", toValue = "available")
    @Query("update Bed b set b.status='available' where b.bedId=:bedId and b.status='reserved'")
    int releaseAsAvailableIfReserved(@Param("bedId") Long bedId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Bed b set b.status='available' where b.bedId=:bedId")
    int releaseAsAvailable(@Param("bedId") Long bedId);
}
