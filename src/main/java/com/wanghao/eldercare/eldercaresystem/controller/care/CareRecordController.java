package com.wanghao.eldercare.eldercaresystem.controller.care;

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
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.care.*;
import com.wanghao.eldercare.eldercaresystem.entity.care.*;
import com.wanghao.eldercare.eldercaresystem.mapper.care.*;
import com.wanghao.eldercare.eldercaresystem.service.care.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<MealIntakeRecord> createMeal(@Valid @RequestBody CreateMealRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.createMeal(user, request));
    }

    @PostMapping("/fluid-records")
    @Audited(action = AuditAction.CREATE, entityType = "fluid_intake_records", responseIdPath = "fluidId",
            requestFields = {"elderId", "drinkType", "volumeMl", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<FluidIntakeRecord> createFluid(@Valid @RequestBody CreateFluidRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.createFluid(user, request));
    }

    @PostMapping("/bowel-records")
    @Audited(action = AuditAction.CREATE, entityType = "bowel_records", responseIdPath = "bowelId",
            requestFields = {"elderId", "bristolType", "amount", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<BowelRecord> createBowel(@Valid @RequestBody CreateBowelRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.createBowel(user, request));
    }

    @PostMapping("/weight-records")
    @Audited(action = AuditAction.CREATE, entityType = "weight_records", responseIdPath = "weightId",
            requestFields = {"elderId", "weightKg", "measureCtx", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<WeightRecord> createWeight(@Valid @RequestBody CreateWeightRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.createWeight(user, request));
    }

    @PutMapping("/meal-records/{mealId}")
    @Audited(action = AuditAction.UPDATE, entityType = "meal_intake_records", entityIdArg = "mealId",
            requestFields = {"elderId", "mealType", "intakeRatio", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<MealIntakeRecord> updateMeal(@PathVariable Long mealId,
                                                    @Valid @RequestBody CreateMealRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.updateMeal(user, mealId, request));
    }

    @DeleteMapping("/meal-records/{mealId}")
    @Audited(action = AuditAction.DELETE, entityType = "meal_intake_records", entityIdArg = "mealId")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<Void> deleteMeal(@PathVariable Long mealId) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        service.deleteMeal(user, mealId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/fluid-records/{fluidId}")
    @Audited(action = AuditAction.UPDATE, entityType = "fluid_intake_records", entityIdArg = "fluidId",
            requestFields = {"elderId", "drinkType", "volumeMl", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<FluidIntakeRecord> updateFluid(@PathVariable Long fluidId,
                                                      @Valid @RequestBody CreateFluidRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.updateFluid(user, fluidId, request));
    }

    @DeleteMapping("/fluid-records/{fluidId}")
    @Audited(action = AuditAction.DELETE, entityType = "fluid_intake_records", entityIdArg = "fluidId")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<Void> deleteFluid(@PathVariable Long fluidId) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        service.deleteFluid(user, fluidId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/bowel-records/{bowelId}")
    @Audited(action = AuditAction.UPDATE, entityType = "bowel_records", entityIdArg = "bowelId",
            requestFields = {"elderId", "bristolType", "amount", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<BowelRecord> updateBowel(@PathVariable Long bowelId,
                                                @Valid @RequestBody CreateBowelRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.updateBowel(user, bowelId, request));
    }

    @DeleteMapping("/bowel-records/{bowelId}")
    @Audited(action = AuditAction.DELETE, entityType = "bowel_records", entityIdArg = "bowelId")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<Void> deleteBowel(@PathVariable Long bowelId) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        service.deleteBowel(user, bowelId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/weight-records/{weightId}")
    @Audited(action = AuditAction.UPDATE, entityType = "weight_records", entityIdArg = "weightId",
            requestFields = {"elderId", "weightKg", "measureCtx", "recordTime"})
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<WeightRecord> updateWeight(@PathVariable Long weightId,
                                                  @Valid @RequestBody CreateWeightRecordRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(service.updateWeight(user, weightId, request));
    }

    @DeleteMapping("/weight-records/{weightId}")
    @Audited(action = AuditAction.DELETE, entityType = "weight_records", entityIdArg = "weightId")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<Void> deleteWeight(@PathVariable Long weightId) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        service.deleteWeight(user, weightId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/meal-records")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<List<MealIntakeRecord>> mealByDate(@RequestParam(required = false) Long elderId,
                                                           @RequestParam(required = false) String date,
                                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        if (from != null || to != null) {
            LocalDate parsedDate = resolveQueryDate(date);
            LocalDateTime effectiveFrom = resolveFrom(parsedDate, from);
            LocalDateTime effectiveTo = resolveTo(parsedDate, to);
            return ApiResponse.ok(service.getMealsByRange(user, elderId, effectiveFrom, effectiveTo));
        }
        if (isAllRange(date)) {
            return ApiResponse.ok(service.getAllMeals(user, elderId));
        }
        if (isLast7DaysRange(date)) {
            return ApiResponse.ok(service.getMealsByRange(user, elderId, recent7DaysFrom(), recent7DaysTo()));
        }
        return ApiResponse.ok(service.getMealsByDate(user, elderId, resolveDate(date)));
    }

    @GetMapping("/fluid-records")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<List<FluidIntakeRecord>> fluidByDate(@RequestParam(required = false) Long elderId,
                                                             @RequestParam(required = false) String date,
                                                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
                                                             @RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        if (from != null || to != null) {
            LocalDate parsedDate = resolveQueryDate(date);
            LocalDateTime effectiveFrom = resolveFrom(parsedDate, from);
            LocalDateTime effectiveTo = resolveTo(parsedDate, to);
            return ApiResponse.ok(service.getFluidsByRange(user, elderId, effectiveFrom, effectiveTo));
        }
        if (isAllRange(date)) {
            return ApiResponse.ok(service.getAllFluids(user, elderId));
        }
        if (isLast7DaysRange(date)) {
            return ApiResponse.ok(service.getFluidsByRange(user, elderId, recent7DaysFrom(), recent7DaysTo()));
        }
        return ApiResponse.ok(service.getFluidsByDate(user, elderId, resolveDate(date)));
    }

    @GetMapping("/bowel-records")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<List<BowelRecord>> bowelByRange(@RequestParam(required = false) Long elderId,
                                                        @RequestParam(required = false) String date,
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        if (from != null || to != null) {
            LocalDate parsedDate = resolveQueryDate(date);
            LocalDateTime effectiveFrom = resolveFrom(parsedDate, from);
            LocalDateTime effectiveTo = resolveTo(parsedDate, to);
            return ApiResponse.ok(service.getBowelsByRange(user, elderId, effectiveFrom, effectiveTo));
        }
        if (isAllRange(date)) {
            return ApiResponse.ok(service.getAllBowels(user, elderId));
        }
        if (isLast7DaysRange(date)) {
            return ApiResponse.ok(service.getBowelsByRange(user, elderId, recent7DaysFrom(), recent7DaysTo()));
        }
        return ApiResponse.ok(service.getBowelsByRange(user, elderId, resolveFrom(resolveDate(date), null), resolveTo(resolveDate(date), null)));
    }

    @GetMapping("/weight-records")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<List<WeightRecord>> weightByRange(@RequestParam(required = false) Long elderId,
                                                          @RequestParam(required = false) String date,
                                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        if (from != null || to != null) {
            LocalDate parsedDate = resolveQueryDate(date);
            LocalDateTime effectiveFrom = resolveFrom(parsedDate, from);
            LocalDateTime effectiveTo = resolveTo(parsedDate, to);
            return ApiResponse.ok(service.getWeightsByRange(user, elderId, effectiveFrom, effectiveTo));
        }
        if (isAllRange(date)) {
            return ApiResponse.ok(service.getAllWeights(user, elderId));
        }
        if (isLast7DaysRange(date)) {
            return ApiResponse.ok(service.getWeightsByRange(user, elderId, recent7DaysFrom(), recent7DaysTo()));
        }
        return ApiResponse.ok(service.getWeightsByRange(user, elderId, resolveFrom(resolveDate(date), null), resolveTo(resolveDate(date), null)));
    }

    private LocalDateTime resolveFrom(LocalDate date, LocalDateTime from) {
        if (from != null) {
            return from;
        }
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        return effectiveDate.atStartOfDay();
    }

    private LocalDateTime resolveTo(LocalDate date, LocalDateTime to) {
        if (to != null) {
            return to;
        }
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        return effectiveDate.atTime(LocalTime.MAX);
    }

    private LocalDate resolveDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(date);
    }

    private LocalDate resolveQueryDate(String date) {
        if (date == null || date.isBlank() || isAllRange(date) || isLast7DaysRange(date)) {
            return null;
        }
        return LocalDate.parse(date);
    }

    private boolean isLast7DaysRange(String date) {
        if (date == null) {
            return false;
        }
        String normalized = date.trim().toLowerCase();
        return normalized.equals("last7days")
                || normalized.equals("last7")
                || normalized.equals("recent7")
                || normalized.equals("7d");
    }

    private boolean isAllRange(String date) {
        return date != null && date.trim().equalsIgnoreCase("all");
    }

    private LocalDateTime recent7DaysFrom() {
        return LocalDate.now().minusDays(6).atStartOfDay();
    }

    private LocalDateTime recent7DaysTo() {
        return LocalDate.now().atTime(LocalTime.MAX);
    }
}
