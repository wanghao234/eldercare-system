package com.wanghao.eldercare.eldercaresystem.care;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateMealRecordRequest {
    @NotNull
    private Long elderId;
    @NotBlank
    private String mealType;
    @NotNull
    @Min(0)
    @Max(100)
    private Integer intakeRatio;
    private String dietType;
    private String note;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public Integer getIntakeRatio() { return intakeRatio; }
    public void setIntakeRatio(Integer intakeRatio) { this.intakeRatio = intakeRatio; }
    public String getDietType() { return dietType; }
    public void setDietType(String dietType) { this.dietType = dietType; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
}
