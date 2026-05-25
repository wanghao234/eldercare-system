package com.wanghao.eldercare.eldercaresystem.dto.medication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class CreateMedicationPlanRequest {

    @NotNull(message = "elderId不能为空")
    private Long elderId;

    @Deprecated
    private Long medicationId;

    @Deprecated
    private String dosage;

    @Deprecated
    private String frequency;

    @Deprecated
    private List<String> times;

    @Valid
    private List<MedicationPlanItemRequest> medicationItems;

    @NotNull(message = "startDate不能为空")
    private LocalDate startDate;

    private LocalDate endDate;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public List<String> getTimes() {
        return times;
    }

    public void setTimes(List<String> times) {
        this.times = times;
    }

    public List<MedicationPlanItemRequest> getMedicationItems() {
        return medicationItems;
    }

    public void setMedicationItems(List<MedicationPlanItemRequest> medicationItems) {
        this.medicationItems = medicationItems;
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
}
