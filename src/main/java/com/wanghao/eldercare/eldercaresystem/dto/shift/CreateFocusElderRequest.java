package com.wanghao.eldercare.eldercaresystem.dto.shift;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.shift.*;
import com.wanghao.eldercare.eldercaresystem.entity.shift.*;
import com.wanghao.eldercare.eldercaresystem.mapper.shift.*;
import com.wanghao.eldercare.eldercaresystem.service.shift.*;
import jakarta.validation.constraints.NotNull;

public class CreateFocusElderRequest {

    @NotNull(message = "elderId 不能为空")
    private Long elderId;

    private String note;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
