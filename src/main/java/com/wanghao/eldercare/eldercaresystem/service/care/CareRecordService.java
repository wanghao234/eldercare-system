package com.wanghao.eldercare.eldercaresystem.service.care;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.care.*;
import com.wanghao.eldercare.eldercaresystem.dto.care.*;
import com.wanghao.eldercare.eldercaresystem.entity.care.*;
import com.wanghao.eldercare.eldercaresystem.mapper.care.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareRecordService {
    private static final Set<String> MEAL_TYPES = Set.of("breakfast", "lunch", "dinner", "snack");
    private static final Set<Integer> INTAKE_RATIOS = Set.of(0, 25, 50, 75, 100);

    private final MealIntakeRecordRepository mealRepo;
    private final FluidIntakeRecordRepository fluidRepo;
    private final BowelRecordRepository bowelRepo;
    private final WeightRecordRepository weightRepo;
    private final PermissionService permissionService;

    public CareRecordService(MealIntakeRecordRepository mealRepo,
                             FluidIntakeRecordRepository fluidRepo,
                             BowelRecordRepository bowelRepo,
                             WeightRecordRepository weightRepo,
                             PermissionService permissionService) {
        this.mealRepo = mealRepo;
        this.fluidRepo = fluidRepo;
        this.bowelRepo = bowelRepo;
        this.weightRepo = weightRepo;
        this.permissionService = permissionService;
    }

    @Transactional
    public MealIntakeRecord createMeal(CurrentUser user, CreateMealRecordRequest request) {
        assertCanRecord(user, request.getElderId());
        validateMeal(request);
        MealIntakeRecord entity = new MealIntakeRecord();
        entity.setElderId(request.getElderId());
        entity.setMealType(normalizeText(request.getMealType()));
        entity.setIntakeRatio(request.getIntakeRatio());
        entity.setDietType(request.getDietType());
        entity.setNote(request.getNote());
        entity.setRecordedBy(user.getUserId());
        entity.setRecordTime(request.getRecordTime() == null ? LocalDateTime.now() : request.getRecordTime());
        entity.setCreatedAt(LocalDateTime.now());
        return mealRepo.save(entity);
    }

    @Transactional
    public FluidIntakeRecord createFluid(CurrentUser user, CreateFluidRecordRequest request) {
        assertCanRecord(user, request.getElderId());
        FluidIntakeRecord entity = new FluidIntakeRecord();
        entity.setElderId(request.getElderId());
        entity.setDrinkType(request.getDrinkType());
        entity.setVolumeMl(request.getVolumeMl());
        entity.setNote(request.getNote());
        entity.setRecordedBy(user.getUserId());
        entity.setRecordTime(request.getRecordTime() == null ? LocalDateTime.now() : request.getRecordTime());
        entity.setCreatedAt(LocalDateTime.now());
        return fluidRepo.save(entity);
    }

    @Transactional
    public BowelRecord createBowel(CurrentUser user, CreateBowelRecordRequest request) {
        assertCanRecord(user, request.getElderId());
        validateBowel(request);
        BowelRecord entity = new BowelRecord();
        entity.setElderId(request.getElderId());
        entity.setBristolType(request.getBristolType());
        entity.setAmount(request.getAmount());
        entity.setIncontinence(request.getIncontinence());
        entity.setBloodFlag(request.getBloodFlag());
        entity.setNote(request.getNote());
        entity.setRecordedBy(user.getUserId());
        entity.setRecordTime(request.getRecordTime() == null ? LocalDateTime.now() : request.getRecordTime());
        entity.setCreatedAt(LocalDateTime.now());
        return bowelRepo.save(entity);
    }

    @Transactional
    public WeightRecord createWeight(CurrentUser user, CreateWeightRecordRequest request) {
        assertCanRecord(user, request.getElderId());
        validateWeight(request);
        WeightRecord entity = new WeightRecord();
        entity.setElderId(request.getElderId());
        entity.setWeightKg(request.getWeightKg());
        entity.setMeasureCtx(request.getMeasureCtx());
        entity.setNote(request.getNote());
        entity.setRecordedBy(user.getUserId());
        entity.setRecordTime(request.getRecordTime() == null ? LocalDateTime.now() : request.getRecordTime());
        entity.setCreatedAt(LocalDateTime.now());
        return weightRepo.save(entity);
    }

    @Transactional
    public MealIntakeRecord updateMeal(CurrentUser user, Long mealId, CreateMealRecordRequest request) {
        MealIntakeRecord entity = mealRepo.findById(mealId).orElseThrow(() -> new NotFoundException("进食记录不存在"));
        assertCanRecord(user, entity.getElderId());
        assertElderUnchanged(entity.getElderId(), request.getElderId());
        validateMeal(request);
        entity.setMealType(normalizeText(request.getMealType()));
        entity.setIntakeRatio(request.getIntakeRatio());
        entity.setDietType(request.getDietType());
        entity.setNote(request.getNote());
        entity.setRecordTime(request.getRecordTime() == null ? entity.getRecordTime() : request.getRecordTime());
        return mealRepo.save(entity);
    }

    @Transactional
    public void deleteMeal(CurrentUser user, Long mealId) {
        MealIntakeRecord entity = mealRepo.findById(mealId).orElseThrow(() -> new NotFoundException("进食记录不存在"));
        assertCanRecord(user, entity.getElderId());
        mealRepo.delete(entity);
    }

    @Transactional
    public FluidIntakeRecord updateFluid(CurrentUser user, Long fluidId, CreateFluidRecordRequest request) {
        FluidIntakeRecord entity = fluidRepo.findById(fluidId).orElseThrow(() -> new NotFoundException("饮水记录不存在"));
        assertCanRecord(user, entity.getElderId());
        assertElderUnchanged(entity.getElderId(), request.getElderId());
        entity.setDrinkType(request.getDrinkType());
        entity.setVolumeMl(request.getVolumeMl());
        entity.setNote(request.getNote());
        entity.setRecordTime(request.getRecordTime() == null ? entity.getRecordTime() : request.getRecordTime());
        return fluidRepo.save(entity);
    }

    @Transactional
    public void deleteFluid(CurrentUser user, Long fluidId) {
        FluidIntakeRecord entity = fluidRepo.findById(fluidId).orElseThrow(() -> new NotFoundException("饮水记录不存在"));
        assertCanRecord(user, entity.getElderId());
        fluidRepo.delete(entity);
    }

    @Transactional
    public BowelRecord updateBowel(CurrentUser user, Long bowelId, CreateBowelRecordRequest request) {
        BowelRecord entity = bowelRepo.findById(bowelId).orElseThrow(() -> new NotFoundException("排便记录不存在"));
        assertCanRecord(user, entity.getElderId());
        assertElderUnchanged(entity.getElderId(), request.getElderId());
        validateBowel(request);
        entity.setBristolType(request.getBristolType());
        entity.setAmount(request.getAmount());
        entity.setIncontinence(request.getIncontinence());
        entity.setBloodFlag(request.getBloodFlag());
        entity.setNote(request.getNote());
        entity.setRecordTime(request.getRecordTime() == null ? entity.getRecordTime() : request.getRecordTime());
        return bowelRepo.save(entity);
    }

    @Transactional
    public void deleteBowel(CurrentUser user, Long bowelId) {
        BowelRecord entity = bowelRepo.findById(bowelId).orElseThrow(() -> new NotFoundException("排便记录不存在"));
        assertCanRecord(user, entity.getElderId());
        bowelRepo.delete(entity);
    }

    @Transactional
    public WeightRecord updateWeight(CurrentUser user, Long weightId, CreateWeightRecordRequest request) {
        WeightRecord entity = weightRepo.findById(weightId).orElseThrow(() -> new NotFoundException("体重记录不存在"));
        assertCanRecord(user, entity.getElderId());
        assertElderUnchanged(entity.getElderId(), request.getElderId());
        validateWeight(request);
        entity.setWeightKg(request.getWeightKg());
        entity.setMeasureCtx(request.getMeasureCtx());
        entity.setNote(request.getNote());
        entity.setRecordTime(request.getRecordTime() == null ? entity.getRecordTime() : request.getRecordTime());
        return weightRepo.save(entity);
    }

    @Transactional
    public void deleteWeight(CurrentUser user, Long weightId) {
        WeightRecord entity = weightRepo.findById(weightId).orElseThrow(() -> new NotFoundException("体重记录不存在"));
        assertCanRecord(user, entity.getElderId());
        weightRepo.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<MealIntakeRecord> getMealsByDate(CurrentUser user, Long elderId, LocalDate date) {
        LocalDateTime from = startOfDay(date);
        LocalDateTime to = endOfDay(date);
        return getMealsByRange(user, elderId, from, to);
    }

    @Transactional(readOnly = true)
    public List<MealIntakeRecord> getMealsByRange(CurrentUser user, Long elderId, LocalDateTime from, LocalDateTime to) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(user, elderId);
            return mealRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from, to);
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(user);
        if (visibleElderIds == null) {
            return mealRepo.findByRecordTimeBetweenOrderByRecordTimeAsc(from, to);
        }
        if (visibleElderIds.isEmpty()) {
            return List.of();
        }
        return mealRepo.findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(visibleElderIds, from, to);
    }

    @Transactional(readOnly = true)
    public List<MealIntakeRecord> getAllMeals(CurrentUser user, Long elderId) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(user, elderId);
            return mealRepo.findByElderIdOrderByRecordTimeAsc(elderId);
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(user);
        if (visibleElderIds == null) {
            return mealRepo.findAll(Sort.by(Sort.Direction.ASC, "recordTime"));
        }
        if (visibleElderIds.isEmpty()) {
            return List.of();
        }
        return mealRepo.findByElderIdInOrderByRecordTimeAsc(visibleElderIds);
    }

    @Transactional(readOnly = true)
    public List<FluidIntakeRecord> getFluidsByDate(CurrentUser user, Long elderId, LocalDate date) {
        LocalDateTime from = startOfDay(date);
        LocalDateTime to = endOfDay(date);
        return getFluidsByRange(user, elderId, from, to);
    }

    @Transactional(readOnly = true)
    public List<FluidIntakeRecord> getFluidsByRange(CurrentUser user, Long elderId, LocalDateTime from, LocalDateTime to) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(user, elderId);
            return fluidRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from, to);
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(user);
        if (visibleElderIds == null) {
            return fluidRepo.findByRecordTimeBetweenOrderByRecordTimeAsc(from, to);
        }
        if (visibleElderIds.isEmpty()) {
            return List.of();
        }
        return fluidRepo.findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(visibleElderIds, from, to);
    }

    @Transactional(readOnly = true)
    public List<FluidIntakeRecord> getAllFluids(CurrentUser user, Long elderId) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(user, elderId);
            return fluidRepo.findByElderIdOrderByRecordTimeAsc(elderId);
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(user);
        if (visibleElderIds == null) {
            return fluidRepo.findAll(Sort.by(Sort.Direction.ASC, "recordTime"));
        }
        if (visibleElderIds.isEmpty()) {
            return List.of();
        }
        return fluidRepo.findByElderIdInOrderByRecordTimeAsc(visibleElderIds);
    }

    @Transactional(readOnly = true)
    public List<BowelRecord> getBowelsByRange(CurrentUser user, Long elderId, LocalDateTime from, LocalDateTime to) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(user, elderId);
            return bowelRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from, to);
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(user);
        if (visibleElderIds == null) {
            return bowelRepo.findByRecordTimeBetweenOrderByRecordTimeAsc(from, to);
        }
        if (visibleElderIds.isEmpty()) {
            return List.of();
        }
        return bowelRepo.findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(visibleElderIds, from, to);
    }

    @Transactional(readOnly = true)
    public List<BowelRecord> getAllBowels(CurrentUser user, Long elderId) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(user, elderId);
            return bowelRepo.findByElderIdOrderByRecordTimeAsc(elderId);
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(user);
        if (visibleElderIds == null) {
            return bowelRepo.findAll(Sort.by(Sort.Direction.ASC, "recordTime"));
        }
        if (visibleElderIds.isEmpty()) {
            return List.of();
        }
        return bowelRepo.findByElderIdInOrderByRecordTimeAsc(visibleElderIds);
    }

    @Transactional(readOnly = true)
    public List<WeightRecord> getWeightsByRange(CurrentUser user, Long elderId, LocalDateTime from, LocalDateTime to) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(user, elderId);
            return weightRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from, to);
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(user);
        if (visibleElderIds == null) {
            return weightRepo.findByRecordTimeBetweenOrderByRecordTimeAsc(from, to);
        }
        if (visibleElderIds.isEmpty()) {
            return List.of();
        }
        return weightRepo.findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(visibleElderIds, from, to);
    }

    @Transactional(readOnly = true)
    public List<WeightRecord> getAllWeights(CurrentUser user, Long elderId) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(user, elderId);
            return weightRepo.findByElderIdOrderByRecordTimeAsc(elderId);
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(user);
        if (visibleElderIds == null) {
            return weightRepo.findAll(Sort.by(Sort.Direction.ASC, "recordTime"));
        }
        if (visibleElderIds.isEmpty()) {
            return List.of();
        }
        return weightRepo.findByElderIdInOrderByRecordTimeAsc(visibleElderIds);
    }

    private void assertCanRecord(CurrentUser user, Long elderId) {
        permissionService.assertCanAccessElder(user, elderId);
        if (user.hasRole("admin") || user.hasRole("nurse_leader") || user.hasRole("nurse") || user.hasRole("caregiver")) {
            return;
        }
        throw new AccessDeniedException("当前角色无权录入照护记录");
    }

    private void validateMeal(CreateMealRecordRequest request) {
        String mealType = normalizeText(request.getMealType());
        if (!MEAL_TYPES.contains(mealType)) {
            throw badRequest("mealType仅支持 breakfast/lunch/dinner/snack");
        }
        if (!INTAKE_RATIOS.contains(request.getIntakeRatio())) {
            throw badRequest("intakeRatio仅支持 0/25/50/75/100");
        }
    }

    private void validateBowel(CreateBowelRecordRequest request) {
        Integer bristolType = request.getBristolType();
        if (bristolType == null || bristolType < 1 || bristolType > 7) {
            throw badRequest("bristolType仅支持1~7");
        }
    }

    private void validateWeight(CreateWeightRecordRequest request) {
        Double weightKg = request.getWeightKg();
        if (weightKg == null || weightKg < 10 || weightKg > 200) {
            throw badRequest("weightKg需在10~200之间");
        }
    }

    private void assertElderUnchanged(Long currentElderId, Long requestElderId) {
        if (!currentElderId.equals(requestElderId)) {
            throw badRequest("elderId不允许修改");
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }
}
