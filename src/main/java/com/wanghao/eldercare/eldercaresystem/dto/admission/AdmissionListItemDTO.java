package com.wanghao.eldercare.eldercaresystem.dto.admission;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.admission.*;
import com.wanghao.eldercare.eldercaresystem.entity.admission.*;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.*;
import com.wanghao.eldercare.eldercaresystem.service.admission.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdmissionListItemDTO {
    private Long admissionId;
    private Long elderId;
    private String elderUsername;
    private String elderName;
    private Long bedId;
    private String bedCode;
    private String contractNo;
    private String packageName;
    private BigDecimal depositAmount;
    private Long processInstanceId;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;

    public static AdmissionListItemDTO from(AdmissionRecord record) {
        AdmissionListItemDTO dto = new AdmissionListItemDTO();
        dto.setAdmissionId(record.getAdmissionId());
        dto.setElderId(record.getElderId());
        dto.setBedId(record.getBedId());
        dto.setContractNo(record.getContractNo());
        dto.setPackageName(record.getPackageName());
        dto.setDepositAmount(record.getDepositAmount());
        dto.setProcessInstanceId(record.getProcessInstanceId());
        dto.setStatus(record.getStatus());
        dto.setStartDate(record.getStartDate());
        dto.setEndDate(record.getEndDate());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getElderUsername() {
        return elderUsername;
    }

    public void setElderUsername(String elderUsername) {
        this.elderUsername = elderUsername;
    }

    public String getElderName() {
        return elderName;
    }

    public void setElderName(String elderName) {
        this.elderName = elderName;
    }

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
    }

    public String getBedCode() {
        return bedCode;
    }

    public void setBedCode(String bedCode) {
        this.bedCode = bedCode;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
