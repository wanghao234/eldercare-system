package com.wanghao.eldercare.eldercaresystem.controller.profile;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.profile.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.profile.*;
import com.wanghao.eldercare.eldercaresystem.dto.profile.ProfilePageResponse;
import com.wanghao.eldercare.eldercaresystem.dto.profile.StaffListItemDTO;
import com.wanghao.eldercare.eldercaresystem.dto.profile.StaffProfileDTO;
import com.wanghao.eldercare.eldercaresystem.dto.profile.StaffProfileUpdateRequest;
import com.wanghao.eldercare.eldercaresystem.entity.profile.*;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.*;
import com.wanghao.eldercare.eldercaresystem.service.profile.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/staff")
public class StaffProfileController {

    private final ProfileService profileService;
    private final CurrentUserUtils currentUserUtils;

    public StaffProfileController(ProfileService profileService, CurrentUserUtils currentUserUtils) {
        this.profileService = profileService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping("/{staffId}")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "staff_profile", entityIdArg = "staffId", sensitive = true)
    public ApiResponse<StaffProfileDTO> getByStaffId(@PathVariable Long staffId,
                                                     @RequestParam(defaultValue = "false") boolean includeSensitive) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(profileService.getStaffProfile(currentUser, staffId, includeSensitive));
    }

    @PutMapping("/{staffId}")
    @Audited(action = AuditAction.UPDATE, entityType = "staff_profile", entityIdArg = "staffId",
            requestFields = {"realName", "jobTitle", "department", "certificationNo", "hireDate", "skills", "phone", "avatarUrl"})
    public ApiResponse<StaffProfileDTO> update(@PathVariable Long staffId,
                                               @Valid @RequestBody StaffProfileUpdateRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(profileService.updateStaffProfile(currentUser, staffId, request));
    }

    @GetMapping
    public ApiResponse<ProfilePageResponse<StaffListItemDTO>> list(@RequestParam(required = false) String role,
                                                                   @RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(profileService.listStaffProfiles(currentUser, role, keyword, status, page, size));
    }
}
