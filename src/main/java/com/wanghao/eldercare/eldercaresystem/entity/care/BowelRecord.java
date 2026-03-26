package com.wanghao.eldercare.eldercaresystem.entity.care;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.care.*;
import com.wanghao.eldercare.eldercaresystem.dto.care.*;
import com.wanghao.eldercare.eldercaresystem.mapper.care.*;
import com.wanghao.eldercare.eldercaresystem.service.care.*;
import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;

@Entity
@Table(name = "bowel_records")
public class BowelRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bowel_id")
    private Long bowelId;

    @Column(name = "elder_id")
    private Long elderId;

    @Column(name = "bristol_type")
    private Integer bristolType;

    @Column(name = "amount")
    private String amount;

    @Column(name = "incontinence")
    private Integer incontinence;

    @Column(name = "blood_flag")
    private Integer bloodFlag;

    @Column(name = "note")
    private String note;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "record_time")
    private LocalDateTime recordTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getBowelId() { return bowelId; }
    public void setBowelId(Long bowelId) { this.bowelId = bowelId; }
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
    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
