package com.wanghao.eldercare.eldercaresystem.elder;

import java.util.List;

public class MealSummaryDTO {
    private Double breakfastAvg;
    private Double lunchAvg;
    private Double dinnerAvg;
    private List<String> missingMeals;

    public Double getBreakfastAvg() { return breakfastAvg; }
    public void setBreakfastAvg(Double breakfastAvg) { this.breakfastAvg = breakfastAvg; }
    public Double getLunchAvg() { return lunchAvg; }
    public void setLunchAvg(Double lunchAvg) { this.lunchAvg = lunchAvg; }
    public Double getDinnerAvg() { return dinnerAvg; }
    public void setDinnerAvg(Double dinnerAvg) { this.dinnerAvg = dinnerAvg; }
    public List<String> getMissingMeals() { return missingMeals; }
    public void setMissingMeals(List<String> missingMeals) { this.missingMeals = missingMeals; }
}
