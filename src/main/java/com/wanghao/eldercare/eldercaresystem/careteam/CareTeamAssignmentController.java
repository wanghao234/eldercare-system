package com.wanghao.eldercare.eldercaresystem.careteam;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
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
@RequestMapping("/api/care-team/assignments")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
public class CareTeamAssignmentController {

    private final CareTeamAssignmentService assignmentService;

    public CareTeamAssignmentController(CareTeamAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public ApiResponse<CareTeamAssignmentPageResponse> list(@RequestParam(required = false) Long elderId,
                                                             @RequestParam(required = false) Long nurseId,
                                                             @RequestParam(required = false) Long familyId,
                                                             @RequestParam(required = false) Integer isActive,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(assignmentService.list(elderId, nurseId, familyId, isActive, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CareTeamAssignmentDTO> detail(@PathVariable Long id) {
        return ApiResponse.ok(assignmentService.detail(id));
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, entityType = "care_team_assignment", responseIdPath = "assignmentId",
            requestFields = {"elderId", "nurseId", "familyId", "isActive"})
    public ApiResponse<CareTeamAssignmentDTO> create(@Valid @RequestBody CreateCareTeamAssignmentRequest request) {
        return ApiResponse.ok(assignmentService.create(request));
    }

    @PutMapping("/{id}")
    @Audited(action = AuditAction.UPDATE, entityType = "care_team_assignment", entityIdArg = "id",
            requestFields = {"elderId", "nurseId", "familyId", "isActive", "unbindNurse"})
    public ApiResponse<CareTeamAssignmentDTO> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateCareTeamAssignmentRequest request) {
        return ApiResponse.ok(assignmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Audited(action = AuditAction.DELETE, entityType = "care_team_assignment", entityIdArg = "id")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return ApiResponse.ok(null);
    }
}
