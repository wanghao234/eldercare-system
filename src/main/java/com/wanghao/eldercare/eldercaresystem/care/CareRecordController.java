package com.wanghao.eldercare.eldercaresystem.care;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/care")
public class CareRecordController {

    private final CareRecordService service;
    private final CurrentUserUtils currentUserUtils;

    public CareRecordController(CareRecordService service, CurrentUserUtils currentUserUtils) {
        this.service = service;
        this.currentUserUtils = currentUserUtils;
    }

    @PostMapping("/meal-records")
    @Audited(action = AuditAction.CREATE, entityType = "meal_intake_records", responseIdPath = "mealId",
            requestFields = {"elderId", "mealType", "intakeRatio", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<MealIntakeRecord> createMeal(@Valid @RequestBody CreateMealRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.createMeal(user, request));
    }

    @PostMapping("/fluid-records")
    @Audited(action = AuditAction.CREATE, entityType = "fluid_intake_records", responseIdPath = "fluidId",
            requestFields = {"elderId", "drinkType", "volumeMl", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<FluidIntakeRecord> createFluid(@Valid @RequestBody CreateFluidRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.createFluid(user, request));
    }

    @PostMapping("/bowel-records")
    @Audited(action = AuditAction.CREATE, entityType = "bowel_records", responseIdPath = "bowelId",
            requestFields = {"elderId", "bristolType", "amount", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<BowelRecord> createBowel(@Valid @RequestBody CreateBowelRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.createBowel(user, request));
    }

    @PostMapping("/weight-records")
    @Audited(action = AuditAction.CREATE, entityType = "weight_records", responseIdPath = "weightId",
            requestFields = {"elderId", "weightKg", "measureCtx", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<WeightRecord> createWeight(@Valid @RequestBody CreateWeightRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.createWeight(user, request));
    }

    @GetMapping("/meal-records")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<List<MealIntakeRecord>> mealByDate(@RequestParam(required = false) Long elderId,
                                                           @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.getMealsByDate(user, elderId, date));
    }

    @GetMapping("/fluid-records")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<List<FluidIntakeRecord>> fluidByDate(@RequestParam(required = false) Long elderId,
                                                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.getFluidsByDate(user, elderId, date));
    }

    @GetMapping("/bowel-records")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<List<BowelRecord>> bowelByRange(@RequestParam(required = false) Long elderId,
                                                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.getBowelsByRange(user, elderId, from, to));
    }

    @GetMapping("/weight-records")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<List<WeightRecord>> weightByRange(@RequestParam(required = false) Long elderId,
                                                          @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                                          @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.getWeightsByRange(user, elderId, from, to));
    }
}
