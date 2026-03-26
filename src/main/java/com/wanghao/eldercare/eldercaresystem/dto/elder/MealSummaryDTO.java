package com.wanghao.eldercare.eldercaresystem.dto.elder;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.elder.*;
import com.wanghao.eldercare.eldercaresystem.service.elder.*;
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
