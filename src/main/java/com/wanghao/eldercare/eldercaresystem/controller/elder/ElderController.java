package com.wanghao.eldercare.eldercaresystem.controller.elder;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtil;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.ElderScoped;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.elder.*;
import com.wanghao.eldercare.eldercaresystem.service.elder.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/elders")
public class ElderController {

    private final CurrentUserUtil currentUserUtil;
    private final ElderService elderService;

    public ElderController(CurrentUserUtil currentUserUtil, ElderService elderService) {
        this.currentUserUtil = currentUserUtil;
        this.elderService = elderService;
    }

    @GetMapping("/{elderId}/profile")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "elder_profile", entityIdArg = "elderId", sensitive = true)
    @ElderScoped(elderIdParam = "elderId")
    public ApiResponse<ElderProfileDTO> getElderProfile(@PathVariable Long elderId) {
        return ApiResponse.success(elderService.getElderProfile(currentUserUtil.getCurrentUser(), elderId));
    }
}
