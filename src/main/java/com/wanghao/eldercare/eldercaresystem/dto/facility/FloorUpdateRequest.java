package com.wanghao.eldercare.eldercaresystem.dto.facility;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.facility.*;
import com.wanghao.eldercare.eldercaresystem.entity.facility.*;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.*;
import com.wanghao.eldercare.eldercaresystem.service.facility.*;
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
