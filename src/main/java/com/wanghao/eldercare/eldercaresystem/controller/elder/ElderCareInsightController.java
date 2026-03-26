package com.wanghao.eldercare.eldercaresystem.controller.elder;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.ElderScoped;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.elder.*;
import com.wanghao.eldercare.eldercaresystem.service.elder.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/elders")
public class ElderCareInsightController {

    private final ElderCareInsightService service;
    private final CurrentUserUtils currentUserUtils;

    public ElderCareInsightController(ElderCareInsightService service, CurrentUserUtils currentUserUtils) {
        this.service = service;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping("/{elderId}/daily-summary")
    @ElderScoped(elderIdParam = "elderId")
    public ApiResponse<DailySummaryDTO> dailySummary(@PathVariable Long elderId,
                                                     @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.getDailySummary(user, elderId, date));
    }

    @GetMapping("/{elderId}/risk-assessment")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "elder_profile", entityIdArg = "elderId", sensitive = true)
    @ElderScoped(elderIdParam = "elderId")
    public ApiResponse<RiskAssessmentDTO> riskAssessment(@PathVariable Long elderId,
                                                         @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.assessRisk(user, elderId, date));
    }
}
