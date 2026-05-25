package com.wanghao.eldercare.eldercaresystem.controller.health;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.ElderScoped;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.health.*;
import com.wanghao.eldercare.eldercaresystem.entity.health.*;
import com.wanghao.eldercare.eldercaresystem.mapper.health.*;
import com.wanghao.eldercare.eldercaresystem.service.health.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health")
public class VitalSignController {

    private final VitalSignService service;
    private final CurrentUserUtils currentUserUtils;

    public VitalSignController(VitalSignService service, CurrentUserUtils currentUserUtils) {
        this.service = service;
        this.currentUserUtils = currentUserUtils;
    }

    @PostMapping("/vitals")
    @Audited(action = AuditAction.CREATE, entityType = "vital_sign_records", responseIdPath = "vitalId",
            requestFields = {"elderId", "recordTime", "temperature", "spo2", "heartRate", "systolicBp", "diastolicBp"})
    @ElderScoped(elderIdParam = "elderId")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<VitalSignRecord> create(@Valid @RequestBody CreateVitalSignRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.create(user, request));
    }

    @PostMapping("/vitals/apple-watch")
    @Audited(action = AuditAction.CREATE, entityType = "vital_sign_records", responseIdPath = "vitalId",
            requestFields = {"elderId", "deviceId", "deviceName", "recordTime", "temperature", "spo2", "heartRate",
                    "systolicBp", "diastolicBp", "bloodGlucose"})
    @PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ELDER)")
    public ApiResponse<VitalSignRecord> createFromAppleWatch(@Valid @RequestBody CreateAppleWatchVitalSignRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.createFromAppleWatch(user, request));
    }

    @PutMapping("/vitals/{vitalId}")
    @Audited(action = AuditAction.UPDATE, entityType = "vital_sign_records", entityIdArg = "vitalId",
            requestFields = {"elderId", "recordTime", "temperature", "spo2", "heartRate", "systolicBp", "diastolicBp"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<VitalSignRecord> update(@PathVariable Long vitalId,
                                               @Valid @RequestBody CreateVitalSignRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.update(user, vitalId, request));
    }

    @DeleteMapping("/vitals/{vitalId}")
    @Audited(action = AuditAction.DELETE, entityType = "vital_sign_records", entityIdArg = "vitalId")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<Void> delete(@PathVariable Long vitalId) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        service.delete(user, vitalId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/vitals")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<List<VitalSignRecord>> list(@RequestParam(required = false) Long elderId,
                                                    @RequestParam(required = false) String date,
                                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        if (from != null || to != null) {
            LocalDate parsedDate = resolveQueryDate(date);
            LocalDateTime effectiveFrom = resolveFrom(parsedDate, from);
            LocalDateTime effectiveTo = resolveTo(parsedDate, to);
            return ApiResponse.ok(service.listByRange(user, elderId, effectiveFrom, effectiveTo));
        }
        if (isAllRange(date)) {
            return ApiResponse.ok(service.listAll(user, elderId));
        }
        if (isLast7DaysRange(date)) {
            return ApiResponse.ok(service.listByRange(user, elderId, recent7DaysFrom(), recent7DaysTo()));
        }
        return ApiResponse.ok(service.listByDate(user, elderId, resolveDate(date)));
    }

    private LocalDateTime resolveFrom(LocalDate date, LocalDateTime from) {
        if (from != null) {
            return from;
        }
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        return effectiveDate.atStartOfDay();
    }

    private LocalDateTime resolveTo(LocalDate date, LocalDateTime to) {
        if (to != null) {
            return to;
        }
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        return effectiveDate.atTime(LocalTime.MAX);
    }

    private LocalDate resolveDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(date);
    }

    private LocalDate resolveQueryDate(String date) {
        if (date == null || date.isBlank() || isAllRange(date) || isLast7DaysRange(date)) {
            return null;
        }
        return LocalDate.parse(date);
    }

    private boolean isLast7DaysRange(String date) {
        if (date == null) {
            return false;
        }
        String normalized = date.trim().toLowerCase();
        return normalized.equals("last7days")
                || normalized.equals("last7")
                || normalized.equals("recent7")
                || normalized.equals("7d");
    }

    private boolean isAllRange(String date) {
        return date != null && date.trim().equalsIgnoreCase("all");
    }

    private LocalDateTime recent7DaysFrom() {
        return LocalDate.now().minusDays(6).atStartOfDay();
    }

    private LocalDateTime recent7DaysTo() {
        return LocalDate.now().atTime(LocalTime.MAX);
    }
}
