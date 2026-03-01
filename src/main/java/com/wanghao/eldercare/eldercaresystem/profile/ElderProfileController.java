package com.wanghao.eldercare.eldercaresystem.profile;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.profile.dto.ElderProfileDTO;
import com.wanghao.eldercare.eldercaresystem.profile.dto.ElderProfileListItemDTO;
import com.wanghao.eldercare.eldercaresystem.profile.dto.ElderProfileUpdateRequest;
import com.wanghao.eldercare.eldercaresystem.profile.dto.ProfilePageResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles/elders")
public class ElderProfileController {

    private final ProfileService profileService;
    private final CurrentUserUtils currentUserUtils;

    public ElderProfileController(ProfileService profileService, CurrentUserUtils currentUserUtils) {
        this.profileService = profileService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping("/{elderId}")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "elder_profile", entityIdArg = "elderId", sensitive = true)
    public ApiResponse<ElderProfileDTO> getByElderId(@PathVariable Long elderId,
                                                     @RequestParam(defaultValue = "false") boolean includeSensitive) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(profileService.getElderProfile(currentUser, elderId, includeSensitive));
    }

    @PutMapping("/{elderId}")
    @Audited(action = AuditAction.UPDATE, entityType = "elder_profile", entityIdArg = "elderId",
            requestFields = {"gender", "birthday", "idNumber", "address", "emergencyContactName",
                    "emergencyContactPhone", "allergies", "chronicConditions", "dietTaboo", "careLevel",
                    "notes", "realName", "phone", "email", "avatarUrl"})
    public ApiResponse<ElderProfileDTO> update(@PathVariable Long elderId,
                                               @Valid @RequestBody ElderProfileUpdateRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(profileService.updateElderProfile(currentUser, elderId, request));
    }

    @GetMapping
    public ApiResponse<ProfilePageResponse<ElderProfileListItemDTO>> list(@RequestParam(required = false) String keyword,
                                                                           @RequestParam(required = false) String careLevel,
                                                                           @RequestParam(required = false) String roomNumber,
                                                                           @RequestParam(required = false) String status,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(profileService.listElderProfiles(currentUser, keyword, careLevel, status, page, size));
    }
}
