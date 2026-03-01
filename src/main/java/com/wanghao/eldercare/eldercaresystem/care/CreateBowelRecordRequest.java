package com.wanghao.eldercare.eldercaresystem.care;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateBowelRecordRequest {
    @NotNull
    private Long elderId;
    @NotNull
    @Min(1)
    @Max(7)
    private Integer bristolType;
    private String amount;
    @NotNull
    private Integer incontinence;
    @NotNull
    private Integer bloodFlag;
    private String note;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Integer getBristolType() { return bristolType; }
    public void setBristolType(Integer bristolType) { this.bristolType = bristolType; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public Integer getIncontinence() { return incontinence; }
    public void setIncontinence(Integer incontinence) { this.incontinence = incontinence; }
    public Integer getBloodFlag() { return bloodFlag; }
    public void setBloodFlag(Integer bloodFlag) { this.bloodFlag = bloodFlag; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
}
