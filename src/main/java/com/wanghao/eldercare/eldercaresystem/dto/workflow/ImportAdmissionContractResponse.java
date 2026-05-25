package com.wanghao.eldercare.eldercaresystem.dto.workflow;

import java.math.BigDecimal;

public class ImportAdmissionContractResponse {

    private Long admissionId;
    private Long elderId;
    private String elderIdNumber;
    private String contractNo;
    private BigDecimal depositAmount;
    private String contractFileUrl;
    private String contractFileName;

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

    public String getElderIdNumber() {
        return elderIdNumber;
    }

    public void setElderIdNumber(String elderIdNumber) {
        this.elderIdNumber = elderIdNumber;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getContractFileUrl() {
        return contractFileUrl;
    }

    public void setContractFileUrl(String contractFileUrl) {
        this.contractFileUrl = contractFileUrl;
    }

    public String getContractFileName() {
        return contractFileName;
    }

    public void setContractFileName(String contractFileName) {
        this.contractFileName = contractFileName;
    }
}
