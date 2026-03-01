package com.wanghao.eldercare.eldercaresystem.facility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FloorCreateRequest {

    @NotNull(message = "buildingId不能为空")
    private Long buildingId;

    @NotBlank(message = "floorNo不能为空")
    private String floorNo;

    private String floorName;

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(String floorNo) {
        this.floorNo = floorNo;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }
}
