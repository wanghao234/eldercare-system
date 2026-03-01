package com.wanghao.eldercare.eldercaresystem.facility;

import jakarta.validation.constraints.NotBlank;

public class FloorUpdateRequest {

    @NotBlank(message = "floorNo不能为空")
    private String floorNo;

    private String floorName;

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
