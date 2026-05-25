package com.wanghao.eldercare.eldercaresystem.dto.medication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class MedicationPlanItemRequest {

    @NotNull(message = "medicationItems.medicationId不能为空")
    private Long medicationId;

    @NotBlank(message = "medicationItems.dosage不能为空")
    private String dosage;

    @NotBlank(message = "medicationItems.frequency不能为空")
    private String frequency;

    @NotEmpty(message = "medicationItems.times不能为空")
    private List<String> times;

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
}
