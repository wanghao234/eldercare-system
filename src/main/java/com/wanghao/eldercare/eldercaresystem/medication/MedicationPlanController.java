package com.wanghao.eldercare.eldercaresystem.medication;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.security.scope.BizScoped;
import com.wanghao.eldercare.eldercaresystem.security.scope.ElderScoped;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medication-plans")
public class MedicationPlanController {

    private final MedicationService medicationService;
    private final CurrentUserUtils currentUserUtils;

    public MedicationPlanController(MedicationService medicationService, CurrentUserUtils currentUserUtils) {
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
    public ApiResponse<MedicationPlanListResponse> list(@RequestParam(required = false) Long elderId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(medicationService.listPlans(currentUser, elderId, status, page, size));
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, entityType = "medication_plans", responseIdPath = "planId",
            requestFields = {"elderId", "medicationId", "dosage", "frequency", "times", "startDate", "endDate"})
    @ElderScoped(elderIdParam = "elderId")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<MedicationPlanDTO> create(@Valid @RequestBody CreateMedicationPlanRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(medicationService.createPlan(currentUser, request));
    }

    @PutMapping("/{planId}")
    @Audited(action = AuditAction.UPDATE, entityType = "medication_plans", entityIdArg = "planId",
            requestFields = {"medicationId", "dosage", "frequency", "times", "startDate", "endDate"})
    @BizScoped(type = "med_plan", idParam = "planId")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<MedicationPlanDTO> update(@PathVariable Long planId,
                                                 @Valid @RequestBody UpdateMedicationPlanRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(medicationService.updatePlan(currentUser, planId, request));
    }

    @PatchMapping("/{planId}/status")
    @Audited(action = AuditAction.TRANSITION, entityType = "medication_plans", entityIdArg = "planId",
            requestFields = {"from", "to"}, fromField = "from", toField = "to")
    @BizScoped(type = "med_plan", idParam = "planId")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<MedicationPlanDTO> patchStatus(@PathVariable Long planId,
                                                      @Valid @RequestBody PatchMedicationPlanStatusRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(medicationService.patchPlanStatus(currentUser, planId, request));
    }
}
