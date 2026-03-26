package com.wanghao.eldercare.eldercaresystem.service.elder;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.elder.*;
import com.wanghao.eldercare.eldercaresystem.dto.elder.*;
import com.wanghao.eldercare.eldercaresystem.entity.care.BowelRecord;
import com.wanghao.eldercare.eldercaresystem.entity.care.FluidIntakeRecord;
import com.wanghao.eldercare.eldercaresystem.entity.health.VitalSignRecord;
import com.wanghao.eldercare.eldercaresystem.mapper.health.VitalSignRecordRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wanghao.eldercare.eldercaresystem.entity.care.MealIntakeRecord;
import com.wanghao.eldercare.eldercaresystem.entity.care.WeightRecord;
import com.wanghao.eldercare.eldercaresystem.mapper.care.BowelRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.care.FluidIntakeRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.care.MealIntakeRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.care.WeightRecordRepository;

@Service
public class ElderCareInsightService {

    private final MealIntakeRecordRepository mealRepo;
    private final FluidIntakeRecordRepository fluidRepo;
    private final BowelRecordRepository bowelRepo;
    private final WeightRecordRepository weightRepo;
    private final VitalSignRecordRepository vitalRepo;
    private final PermissionService permissionService;

    public ElderCareInsightService(MealIntakeRecordRepository mealRepo,
                                   FluidIntakeRecordRepository fluidRepo,
                                   BowelRecordRepository bowelRepo,
                                   WeightRecordRepository weightRepo,
                                   VitalSignRecordRepository vitalRepo,
                                   PermissionService permissionService) {
        this.mealRepo = mealRepo;
        this.fluidRepo = fluidRepo;
        this.bowelRepo = bowelRepo;
        this.weightRepo = weightRepo;
        this.vitalRepo = vitalRepo;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public DailySummaryDTO getDailySummary(CurrentUser user, Long elderId, LocalDate date) {
        permissionService.assertCanAccessElder(user, elderId);
        LocalDateTime from = start(date);
        LocalDateTime to = end(date);

        List<MealIntakeRecord> meals = mealRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from, to);
        List<FluidIntakeRecord> fluids = fluidRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from, to);
        List<BowelRecord> bowels = bowelRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from, to);
        List<WeightRecord> weights = weightRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from.minusDays(7), to);
        VitalSignRecord latestVital = vitalRepo.findTopByElderIdAndRecordTimeBetweenOrderByRecordTimeDesc(elderId, from.minusDays(7), to).orElse(null);

        MealSummaryDTO mealSummary = buildMealSummary(meals);
        Integer fluidTotal = fluids.stream().map(FluidIntakeRecord::getVolumeMl).filter(v -> v != null).reduce(0, Integer::sum);

        DailySummaryDTO dto = new DailySummaryDTO();
        dto.setDate(date);
        dto.setMealSummary(mealSummary);
        dto.setFluidTotalMl(fluidTotal);
        dto.setBowelCount(bowels.size());
        dto.setLatestBristolType(bowels.isEmpty() ? null : bowels.get(bowels.size() - 1).getBristolType());
        dto.setWeightLatest(weights.isEmpty() ? null : weights.get(weights.size() - 1).getWeightKg());
        dto.setVitalsLatest(toVitalSnapshot(latestVital));

        List<String> missing = new ArrayList<>();
        if (meals.isEmpty()) missing.add("meal");
        if (fluids.isEmpty()) missing.add("fluid");
        if (bowels.isEmpty()) missing.add("bowel");
        if (latestVital == null) missing.add("vitals");
        dto.setMissingFlags(missing);
        return dto;
    }

    @Transactional(readOnly = true)
    public RiskAssessmentDTO assessRisk(CurrentUser user, Long elderId, LocalDate date) {
        permissionService.assertCanAccessElder(user, elderId);

        LocalDateTime dayFrom = start(date);
        LocalDateTime dayTo = end(date);
        LocalDateTime prevFrom = start(date.minusDays(1));
        LocalDateTime prevTo = end(date.minusDays(1));
        LocalDateTime twoDayFrom = dayFrom.minusDays(1);

        List<String> reasons = new ArrayList<>();

        int dayFluid = sumFluid(elderId, dayFrom, dayTo);
        int prevFluid = sumFluid(elderId, prevFrom, prevTo);
        String dehydrationRisk = "low";
        if (dayFluid < 800 && prevFluid < 800) {
            dehydrationRisk = "high";
            reasons.add("连续2天饮水<800ml");
        } else if (dayFluid < 800) {
            dehydrationRisk = "medium";
            reasons.add("当日饮水<800ml");
        }

        List<BowelRecord> bowel48h = bowelRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, twoDayFrom, dayTo);
        String constipationRisk = "low";
        if (bowel48h.isEmpty()) {
            constipationRisk = "high";
            reasons.add("48小时无排便记录");
        } else {
            Integer lastType = bowel48h.get(bowel48h.size() - 1).getBristolType();
            if (lastType != null && (lastType == 1 || lastType == 2)) {
                constipationRisk = "medium";
                reasons.add("最近排便 Bristol 1-2");
            }
        }

        double dayMealAvg = mealMainAvg(elderId, dayFrom, dayTo);
        double prevMealAvg = mealMainAvg(elderId, prevFrom, prevTo);
        String nutritionRisk = "low";
        if (dayMealAvg < 50 && prevMealAvg < 50) {
            nutritionRisk = "high";
            reasons.add("连续2天主餐平均摄入<50%");
        } else if (dayMealAvg < 50) {
            nutritionRisk = "medium";
            reasons.add("当日主餐平均摄入<50%");
        }

        String weightChangeRisk = evaluateWeightRisk(elderId, dayTo, reasons);
        String abnormalVitalRisk = evaluateVitalRisk(elderId, dayFrom.minusDays(7), dayTo, reasons);

        RiskAssessmentDTO dto = new RiskAssessmentDTO();
        dto.setDehydrationRisk(dehydrationRisk);
        dto.setConstipationRisk(constipationRisk);
        dto.setNutritionRisk(nutritionRisk);
        dto.setWeightChangeRisk(weightChangeRisk);
        dto.setAbnormalVitalRisk(abnormalVitalRisk);
        dto.setReasons(reasons);
        return dto;
    }

    private String evaluateWeightRisk(Long elderId, LocalDateTime now, List<String> reasons) {
        List<WeightRecord> days7 = weightRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, now.minusDays(7), now);
        List<WeightRecord> days30 = weightRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, now.minusDays(30), now);
        if (days7.size() < 2 && days30.size() < 2) {
            return "low";
        }

        double d7 = diff(days7);
        double pct30 = percentDiff(days30);
        if (Math.abs(d7) > 2 || Math.abs(pct30) > 5) {
            reasons.add("体重7天变化>2kg或30天变化>5%");
            return "high";
        }
        if (Math.abs(d7) > 1 || Math.abs(pct30) > 3) {
            reasons.add("体重变化偏快");
            return "medium";
        }
        return "low";
    }

    private String evaluateVitalRisk(Long elderId, LocalDateTime from, LocalDateTime to, List<String> reasons) {
        VitalSignRecord latest = vitalRepo.findTopByElderIdAndRecordTimeBetweenOrderByRecordTimeDesc(elderId, from, to).orElse(null);
        if (latest == null) {
            return "low";
        }

        if ((latest.getTemperature() != null && latest.getTemperature() > 38)
                || (latest.getSpo2() != null && latest.getSpo2() < 90)
                || (latest.getHeartRate() != null && latest.getHeartRate() > 130)
                || (latest.getSystolicBp() != null && latest.getSystolicBp() > 180)
                || (latest.getDiastolicBp() != null && latest.getDiastolicBp() > 110)) {
            reasons.add("体征明显异常");
            return "high";
        }

        if ((latest.getTemperature() != null && latest.getTemperature() > 37.5)
                || (latest.getSpo2() != null && latest.getSpo2() < 92)
                || (latest.getHeartRate() != null && latest.getHeartRate() > 120)
                || (latest.getSystolicBp() != null && latest.getSystolicBp() > 160)
                || (latest.getDiastolicBp() != null && latest.getDiastolicBp() > 100)) {
            reasons.add("体征轻度异常");
            return "medium";
        }

        return "low";
    }

    private MealSummaryDTO buildMealSummary(List<MealIntakeRecord> meals) {
        MealSummaryDTO mealSummary = new MealSummaryDTO();
        mealSummary.setBreakfastAvg(avg(meals, "breakfast"));
        mealSummary.setLunchAvg(avg(meals, "lunch"));
        mealSummary.setDinnerAvg(avg(meals, "dinner"));

        List<String> missingMeals = new ArrayList<>();
        if (mealSummary.getBreakfastAvg() == null) missingMeals.add("breakfast");
        if (mealSummary.getLunchAvg() == null) missingMeals.add("lunch");
        if (mealSummary.getDinnerAvg() == null) missingMeals.add("dinner");
        mealSummary.setMissingMeals(missingMeals);
        return mealSummary;
    }

    private VitalSnapshotDTO toVitalSnapshot(VitalSignRecord vital) {
        if (vital == null) {
            return null;
        }
        VitalSnapshotDTO dto = new VitalSnapshotDTO();
        dto.setRecordTime(vital.getRecordTime());
        dto.setHeartRate(vital.getHeartRate());
        dto.setSystolicBp(vital.getSystolicBp());
        dto.setDiastolicBp(vital.getDiastolicBp());
        dto.setSpo2(vital.getSpo2());
        dto.setTemperature(vital.getTemperature());
        return dto;
    }

    private int sumFluid(Long elderId, LocalDateTime from, LocalDateTime to) {
        return fluidRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from, to)
                .stream().map(FluidIntakeRecord::getVolumeMl).filter(v -> v != null).reduce(0, Integer::sum);
    }

    private double mealMainAvg(Long elderId, LocalDateTime from, LocalDateTime to) {
        List<MealIntakeRecord> meals = mealRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from, to);
        List<String> major = Arrays.asList("breakfast", "lunch", "dinner");
        return meals.stream()
                .filter(m -> m.getMealType() != null && major.contains(m.getMealType().toLowerCase(Locale.ROOT)))
                .map(MealIntakeRecord::getIntakeRatio)
                .filter(i -> i != null)
                .mapToInt(Integer::intValue)
                .average().orElse(100.0);
    }

    private Double avg(List<MealIntakeRecord> meals, String mealType) {
        return meals.stream()
                .filter(m -> m.getMealType() != null && mealType.equalsIgnoreCase(m.getMealType()))
                .map(MealIntakeRecord::getIntakeRatio)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .average().isPresent()
                ? meals.stream().filter(m -> m.getMealType() != null && mealType.equalsIgnoreCase(m.getMealType()))
                    .map(MealIntakeRecord::getIntakeRatio).filter(v -> v != null)
                    .mapToInt(Integer::intValue).average().orElse(0)
                : null;
    }

    private double diff(List<WeightRecord> records) {
        if (records.size() < 2) return 0;
        Double first = records.get(0).getWeightKg();
        Double last = records.get(records.size() - 1).getWeightKg();
        if (first == null || last == null) return 0;
        return last - first;
    }

    private double percentDiff(List<WeightRecord> records) {
        if (records.size() < 2) return 0;
        Double first = records.get(0).getWeightKg();
        Double last = records.get(records.size() - 1).getWeightKg();
        if (first == null || last == null || first == 0) return 0;
        return (last - first) * 100.0 / first;
    }

    private LocalDateTime start(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime end(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }
}
