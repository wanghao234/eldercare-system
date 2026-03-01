package com.wanghao.eldercare.eldercaresystem.shift;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.security.Role;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
public class ShiftController {

    private final ShiftService shiftService;
    private final CurrentUserUtils currentUserUtils;

    public ShiftController(ShiftService shiftService, CurrentUserUtils currentUserUtils) {
        this.shiftService = shiftService;
        this.currentUserUtils = currentUserUtils;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.CREATE, entityType = "shifts", responseIdPath = "shiftId",
            requestFields = {"shiftDate", "shiftType", "leaderId"})
    public ApiResponse<Shift> createShift(@Valid @RequestBody CreateShiftRequest request) {
        return ApiResponse.ok(shiftService.createShift(request));
    }

    @GetMapping
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "shifts", sensitive = true)
    public ApiResponse<ShiftPageResponse<Shift>> listShifts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String shiftType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(shiftService.listShifts(from, to, shiftType, status, page, size));
    }

    @GetMapping("/{shiftId}")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "shifts", entityIdArg = "shiftId", sensitive = true)
    public ApiResponse<Shift> getShift(@PathVariable Long shiftId) {
        return ApiResponse.ok(shiftService.getShift(shiftId));
    }

    @PostMapping("/{shiftId}/close")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.TRANSITION, entityType = "shifts", entityIdArg = "shiftId", fromField = "from", toField = "to")
    public ApiResponse<Shift> closeShift(@PathVariable Long shiftId,
                                         @Valid @RequestBody CloseShiftRequest request) {
        return ApiResponse.ok(shiftService.closeShift(shiftId, request));
    }

    @PostMapping("/{shiftId}/handover-notes")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    @Audited(action = AuditAction.CREATE, entityType = "handover_notes", responseIdPath = "noteId", requestFields = {"content"})
    public ApiResponse<HandoverNote> createHandoverNote(@PathVariable Long shiftId,
                                                        @Valid @RequestBody CreateHandoverNoteRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(shiftService.createHandoverNote(currentUser, shiftId, request));
    }

    @GetMapping("/{shiftId}/handover-notes")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "handover_notes", entityIdArg = "shiftId", sensitive = true)
    public ApiResponse<ShiftPageResponse<HandoverNote>> listHandoverNotes(@PathVariable Long shiftId,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(shiftService.listHandoverNotes(shiftId, page, size));
    }

    @PostMapping("/{shiftId}/focus-elders")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.CREATE, entityType = "handover_focus_elders",
            requestFields = {"elderId", "note"})
    public ApiResponse<HandoverFocusElder> addFocusElder(@PathVariable Long shiftId,
                                                         @Valid @RequestBody CreateFocusElderRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(shiftService.addFocusElder(currentUser, shiftId, request));
    }

    @GetMapping("/{shiftId}/focus-elders")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "handover_focus_elders", entityIdArg = "shiftId", sensitive = true)
    public ApiResponse<List<HandoverFocusElder>> listFocusElders(@PathVariable Long shiftId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(shiftService.listFocusElders(currentUser, shiftId));
    }

    @DeleteMapping("/{shiftId}/focus-elders/{elderId}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.DELETE, entityType = "handover_focus_elders", entityIdArg = "shiftId")
    public ApiResponse<Void> deleteFocusElder(@PathVariable Long shiftId, @PathVariable Long elderId) {
        shiftService.deleteFocusElder(shiftId, elderId);
        return ApiResponse.ok(null);
    }
}
