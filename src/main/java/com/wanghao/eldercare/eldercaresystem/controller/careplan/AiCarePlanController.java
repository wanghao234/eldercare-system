package com.wanghao.eldercare.eldercaresystem.controller.careplan;

import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.AiCarePlanDraftDTO;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.AiCarePlanGenerateRequest;
import com.wanghao.eldercare.eldercaresystem.service.careplan.AiCarePlanService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/care-plan")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_DOCTOR)")
public class AiCarePlanController {

    private final AiCarePlanService aiCarePlanService;
    private final CurrentUserUtils currentUserUtils;

    public AiCarePlanController(AiCarePlanService aiCarePlanService, CurrentUserUtils currentUserUtils) {
        this.aiCarePlanService = aiCarePlanService;
        this.currentUserUtils = currentUserUtils;
    }

    @PostMapping("/generate")
    public ApiResponse<AiCarePlanDraftDTO> generate(@Valid @RequestBody AiCarePlanGenerateRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("AI护理计划草稿生成成功", aiCarePlanService.generateDraft(currentUser, request));
    }
}
