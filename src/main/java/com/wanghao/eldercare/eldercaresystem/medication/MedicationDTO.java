package com.wanghao.eldercare.eldercaresystem.medication;

import java.time.LocalDateTime;

public class MedicationDTO {
    private Long medicationId;
    private String medicationName;
    private String spec;
    private String unit;
    private String description;
    private LocalDateTime createdAt;

    public static MedicationDTO from(Medication medication) {
        MedicationDTO dto = new MedicationDTO();
        dto.setMedicationId(medication.getMedicationId());
        dto.setMedicationName(medication.getMedicationName());
        dto.setSpec(medication.getSpec());
        dto.setUnit(medication.getUnit());
        dto.setDescription(medication.getDescription());
        dto.setCreatedAt(medication.getCreatedAt());
        return dto;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
