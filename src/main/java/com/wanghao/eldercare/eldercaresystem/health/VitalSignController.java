package com.wanghao.eldercare.eldercaresystem.health;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.security.scope.ElderScoped;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

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
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<VitalSignRecord> create(@Valid @RequestBody CreateVitalSignRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.create(user, request));
    }

    @GetMapping("/vitals")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<List<VitalSignRecord>> list(@RequestParam(required = false) Long elderId,
                                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.listByRange(user, elderId, from, to));
    }
}
