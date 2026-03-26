package com.wanghao.eldercare.eldercaresystem.dto.qc;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.qc.*;
import com.wanghao.eldercare.eldercaresystem.entity.qc.*;
import com.wanghao.eldercare.eldercaresystem.mapper.qc.*;
import com.wanghao.eldercare.eldercaresystem.service.qc.*;

public class CreateQcAuditRequest {
    private Long elderId;
    private String title;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
