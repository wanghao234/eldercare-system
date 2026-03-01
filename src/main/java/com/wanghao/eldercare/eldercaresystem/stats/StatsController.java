package com.wanghao.eldercare.eldercaresystem.stats;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/stats")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/alarms")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "stats", sensitive = true)
    public ApiResponse<AlarmStatsResponse> alarmStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.ok(statsService.alarmStats(from, to));
    }

    @GetMapping("/tasks")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "stats", sensitive = true)
    public ApiResponse<TaskStatsResponse> taskStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.ok(statsService.taskStats(from, to));
    }

    @GetMapping("/medication")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "stats", sensitive = true)
    public ApiResponse<MedicationStatsResponse> medicationStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.ok(statsService.medicationStats(from, to));
    }

    @GetMapping("/occupancy")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "stats", sensitive = true)
    public ApiResponse<OccupancyStatsResponse> occupancyStats() {
        return ApiResponse.ok(statsService.occupancyStats());
    }
}

