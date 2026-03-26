package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.*;
import com.wanghao.eldercare.eldercaresystem.service.careplan.*;

public class RegenerateTasksResponse {
    private Long carePlanId;
    private Integer deletedTaskCount;
    private Integer generatedTaskCount;

    public Long getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(Long carePlanId) {
        this.carePlanId = carePlanId;
    }

    public Integer getDeletedTaskCount() {
        return deletedTaskCount;
    }

    public void setDeletedTaskCount(Integer deletedTaskCount) {
        this.deletedTaskCount = deletedTaskCount;
    }

    public Integer getGeneratedTaskCount() {
        return generatedTaskCount;
    }

    public void setGeneratedTaskCount(Integer generatedTaskCount) {
        this.generatedTaskCount = generatedTaskCount;
    }
}
