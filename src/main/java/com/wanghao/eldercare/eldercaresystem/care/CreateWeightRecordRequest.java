package com.wanghao.eldercare.eldercaresystem.care;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateWeightRecordRequest {
    @NotNull
    private Long elderId;
    @NotNull
    private Double weightKg;
    private String measureCtx;
    private String note;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public String getMeasureCtx() { return measureCtx; }
    public void setMeasureCtx(String measureCtx) { this.measureCtx = measureCtx; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
}
