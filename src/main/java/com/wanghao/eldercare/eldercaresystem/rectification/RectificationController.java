package com.wanghao.eldercare.eldercaresystem.rectification;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtil;
import com.wanghao.eldercare.eldercaresystem.security.perm.RequirePerm;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
public class RectificationController {

    private final RectificationService rectificationService;
    private final CurrentUserUtil currentUserUtil;

    public RectificationController(RectificationService rectificationService, CurrentUserUtil currentUserUtil) {
        this.rectificationService = rectificationService;
        this.currentUserUtil = currentUserUtil;
    }

    @PostMapping("/rectifications")
    @RequirePerm("rectification:handle")
    @Audited(action = AuditAction.CREATE, entityType = "rectifications", responseIdPath = "rectificationId", requestFields = {"sourceType", "sourceId", "title", "level", "ownerId", "dueAt"})
    public ApiResponse<RectificationCreateResponse> create(@Valid @RequestBody CreateRectificationRequest request) {
        CurrentUser currentUser = currentUserUtil.getCurrentUser();
        return ApiResponse.ok(rectificationService.create(currentUser, request));
    }

    @GetMapping("/rectifications")
    @RequirePerm("rectification:read")
    public ApiResponse<RectificationListResponse> list(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String level,
                                                       @RequestParam(required = false) Long ownerId,
                                                       @RequestParam(required = false) String sourceType,
                                                       @RequestParam(required = false) Long sourceId) {
        CurrentUser currentUser = currentUserUtil.getCurrentUser();
        return ApiResponse.ok(rectificationService.list(currentUser, status, level, ownerId, sourceType, sourceId, page, size));
    }

    @GetMapping("/rectifications/{id}")
    @RequirePerm("rectification:read")
    public ApiResponse<RectificationDetailDTO> detail(@PathVariable Long id) {
        CurrentUser currentUser = currentUserUtil.getCurrentUser();
        return ApiResponse.ok(rectificationService.detail(currentUser, id));
    }

    @PostMapping("/rectifications/{id}/transition")
    @RequirePerm("rectification:handle")
    @Audited(
            action = AuditAction.TRANSITION,
            entityType = "rectifications",
            entityIdArg = "id",
            fromField = "from",
            toField = "to",
            requestFields = {"from", "to", "comment"}
    )
    public ApiResponse<RectificationDetailDTO> transition(@PathVariable Long id,
                                                          @Valid @RequestBody RectificationTransitionRequest request) {
        CurrentUser currentUser = currentUserUtil.getCurrentUser();
        return ApiResponse.ok(rectificationService.transition(currentUser, id, request));
    }

    @PostMapping("/rectifications/{id}/actions")
    @RequirePerm("rectification:handle")
    @Audited(action = AuditAction.UPDATE, entityType = "rectifications", entityIdArg = "id", requestFields = {"actionType", "content"})
    public ApiResponse<RectificationActionDTO> addAction(@PathVariable Long id,
                                                         @Valid @RequestBody CreateRectificationActionRequest request) {
        CurrentUser currentUser = currentUserUtil.getCurrentUser();
        return ApiResponse.ok(rectificationService.addAction(currentUser, id, request));
    }

}
