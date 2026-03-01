package com.wanghao.eldercare.eldercaresystem.elder;

import java.time.LocalDate;
import java.util.List;

public class DailySummaryDTO {
    private LocalDate date;
    private MealSummaryDTO mealSummary;
    private Integer fluidTotalMl;
    private Integer bowelCount;
    private Integer latestBristolType;
    private Double weightLatest;
    private VitalSnapshotDTO vitalsLatest;
    private List<String> missingFlags;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public MealSummaryDTO getMealSummary() { return mealSummary; }
    public void setMealSummary(MealSummaryDTO mealSummary) { this.mealSummary = mealSummary; }
    public Integer getFluidTotalMl() { return fluidTotalMl; }
    public void setFluidTotalMl(Integer fluidTotalMl) { this.fluidTotalMl = fluidTotalMl; }
    public Integer getBowelCount() { return bowelCount; }
    public void setBowelCount(Integer bowelCount) { this.bowelCount = bowelCount; }
    public Integer getLatestBristolType() { return latestBristolType; }
    public void setLatestBristolType(Integer latestBristolType) { this.latestBristolType = latestBristolType; }
    public Double getWeightLatest() { return weightLatest; }
    public void setWeightLatest(Double weightLatest) { this.weightLatest = weightLatest; }
    public VitalSnapshotDTO getVitalsLatest() { return vitalsLatest; }
    public void setVitalsLatest(VitalSnapshotDTO vitalsLatest) { this.vitalsLatest = vitalsLatest; }
    public List<String> getMissingFlags() { return missingFlags; }
    public void setMissingFlags(List<String> missingFlags) { this.missingFlags = missingFlags; }
}
