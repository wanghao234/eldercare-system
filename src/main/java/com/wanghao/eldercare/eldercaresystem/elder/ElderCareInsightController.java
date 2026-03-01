package com.wanghao.eldercare.eldercaresystem.elder;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.security.scope.ElderScoped;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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
