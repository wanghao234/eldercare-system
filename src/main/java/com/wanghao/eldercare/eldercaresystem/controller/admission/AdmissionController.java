package com.wanghao.eldercare.eldercaresystem.controller.admission;

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
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.admission.*;
import com.wanghao.eldercare.eldercaresystem.entity.admission.*;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.*;
import com.wanghao.eldercare.eldercaresystem.service.admission.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AdmissionController {

    private final AdmissionService admissionService;
    private final CurrentUserUtils currentUserUtils;

    public AdmissionController(AdmissionService admissionService, CurrentUserUtils currentUserUtils) {
        this.admissionService = admissionService;
        this.currentUserUtils = currentUserUtils;
    }

    @PostMapping("/admissions")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.CREATE, entityType = "admission_records", responseIdPath = "id")
    public ApiResponse<IdResponse> createAdmission(@Valid @RequestBody CreateAdmissionRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(admissionService.createAdmission(currentUser, request));
    }

    @GetMapping("/admissions")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_DOCTOR,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_FAMILY)")
    public ApiResponse<AdmissionListResponse> listAdmissions(@RequestParam(required = false) Long elderId,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(admissionService.listAdmissions(currentUser, elderId, status, page, size));
    }

    @GetMapping("/admissions/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_DOCTOR,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_FAMILY)")
    public ApiResponse<AdmissionRecord> getAdmission(@PathVariable Long id) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(admissionService.getAdmissionDetail(currentUser, id));
    }

    @PostMapping("/discharges")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.CREATE, entityType = "discharge_records", responseIdPath = "id")
    public ApiResponse<IdResponse> createDischarge(@Valid @RequestBody DischargeCreateRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(admissionService.createDischarge(currentUser, request));
    }

    @GetMapping("/discharges")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_DOCTOR,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_FAMILY)")
    public ApiResponse<DischargeListResponse> listDischarges(@RequestParam(required = false) Long admissionId,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(admissionService.listDischarges(currentUser, admissionId, status, page, size));
    }

    @GetMapping("/discharges/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_DOCTOR,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_FAMILY)")
    public ApiResponse<DischargeRecord> getDischarge(@PathVariable Long id) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(admissionService.getDischargeDetail(currentUser, id));
    }

    @PostMapping("/discharges/{id}/settlement")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.TRANSITION, entityType = "discharge_records", entityIdArg = "id", fromValue = "pending", toValue = "settling")
    public ApiResponse<DischargeRecord> settlement(@PathVariable Long id,
                                                   @Valid @RequestBody SettlementRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(admissionService.settlement(currentUser, id, request));
    }

    @PostMapping("/discharges/{id}/complete")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.TRANSITION, entityType = "discharge_records", entityIdArg = "id", fromValue = "settling|approved", toValue = "completed")
    public ApiResponse<DischargeRecord> complete(@PathVariable Long id,
                                                 @Valid @RequestBody CompleteDischargeRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(admissionService.completeDischarge(currentUser, id, request));
    }
}
