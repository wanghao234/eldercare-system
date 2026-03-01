package com.wanghao.eldercare.eldercaresystem.facility;

import jakarta.validation.constraints.NotBlank;

public class BuildingUpsertRequest {

    @NotBlank(message = "buildingName不能为空")
    private String buildingName;

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }
}
