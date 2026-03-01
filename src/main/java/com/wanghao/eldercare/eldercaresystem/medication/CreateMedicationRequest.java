package com.wanghao.eldercare.eldercaresystem.medication;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class CreateMedicationRequest {

    @JsonAlias({"name", "medication", "medication_name", "drugName", "medicineName"})
    @NotBlank(message = "medicationName不能为空")
    private String medicationName;

    private String spec;
    private String unit;
    private String description;

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
