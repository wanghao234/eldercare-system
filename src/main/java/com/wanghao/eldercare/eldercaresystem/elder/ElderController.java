package com.wanghao.eldercare.eldercaresystem.elder;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtil;
import com.wanghao.eldercare.eldercaresystem.security.scope.ElderScoped;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
