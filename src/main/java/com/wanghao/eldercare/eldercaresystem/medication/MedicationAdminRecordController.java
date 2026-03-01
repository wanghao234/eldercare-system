package com.wanghao.eldercare.eldercaresystem.medication;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
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

@RestController
@RequestMapping("/api/medication-records")
public class MedicationAdminRecordController {

    private final MedicationService medicationService;
    private final CurrentUserUtils currentUserUtils;

    public MedicationAdminRecordController(MedicationService medicationService, CurrentUserUtils currentUserUtils) {
        this.medicationService = medicationService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_FAMILY,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ELDER)")
    public ApiResponse<MedicationAdminRecordListResponse> list(@RequestParam(required = false) Long elderId,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(medicationService.listRecords(currentUser, elderId, from, to, status, page, size));
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, entityType = "medication_admin_records", responseIdPath = "recordId",
            requestFields = {"elderId", "medicationId", "planId", "administeredTime", "status", "dosage", "note"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<MedicationAdminRecordDTO> create(@Valid @RequestBody CreateMedicationAdminRecordRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(medicationService.createRecord(currentUser, request));
    }
}
