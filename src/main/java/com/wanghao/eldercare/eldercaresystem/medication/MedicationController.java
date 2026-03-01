package com.wanghao.eldercare.eldercaresystem.medication;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationService medicationService;
    private final CurrentUserUtils currentUserUtils;

    public MedicationController(MedicationService medicationService, CurrentUserUtils currentUserUtils) {
        this.medicationService = medicationService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<MedicationListResponse> list(@RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(medicationService.listMedications(keyword, page, size));
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, entityType = "medications", responseIdPath = "medicationId",
            requestFields = {"medicationName", "spec", "unit", "description"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN)")
    public ApiResponse<MedicationDTO> create(@Valid @RequestBody CreateMedicationRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(medicationService.createMedication(currentUser, request));
    }

    @PutMapping("/{id}")
    @Audited(action = AuditAction.UPDATE, entityType = "medications", entityIdArg = "id",
            requestFields = {"medicationName", "spec", "unit", "description"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN)")
    public ApiResponse<MedicationDTO> update(@PathVariable Long id,
                                             @Valid @RequestBody CreateMedicationRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(medicationService.updateMedication(currentUser, id, request));
    }

    @DeleteMapping("/{id}")
    @Audited(action = AuditAction.DELETE, entityType = "medications", entityIdArg = "id")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        medicationService.deleteMedication(currentUser, id);
        return ApiResponse.ok(null);
    }
}
