package com.wanghao.eldercare.eldercaresystem.elder;

import java.util.List;

public class RiskAssessmentDTO {
    private String dehydrationRisk;
    private String constipationRisk;
    private String nutritionRisk;
    private String weightChangeRisk;
    private String abnormalVitalRisk;
    private List<String> reasons;

    public String getDehydrationRisk() { return dehydrationRisk; }
    public void setDehydrationRisk(String dehydrationRisk) { this.dehydrationRisk = dehydrationRisk; }
    public String getConstipationRisk() { return constipationRisk; }
    public void setConstipationRisk(String constipationRisk) { this.constipationRisk = constipationRisk; }
    public String getNutritionRisk() { return nutritionRisk; }
    public void setNutritionRisk(String nutritionRisk) { this.nutritionRisk = nutritionRisk; }
    public String getWeightChangeRisk() { return weightChangeRisk; }
    public void setWeightChangeRisk(String weightChangeRisk) { this.weightChangeRisk = weightChangeRisk; }
    public String getAbnormalVitalRisk() { return abnormalVitalRisk; }
    public void setAbnormalVitalRisk(String abnormalVitalRisk) { this.abnormalVitalRisk = abnormalVitalRisk; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
}
