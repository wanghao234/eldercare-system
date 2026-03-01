package com.wanghao.eldercare.eldercaresystem.admission;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class DischargeCreateRequest {

    @NotNull(message = "admissionId 不能为空")
    private Long admissionId;

    private String reason;
    private LocalDate requestedDate;

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }
}
