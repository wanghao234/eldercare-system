package com.wanghao.eldercare.eldercaresystem.dto.workflow;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.workflow.*;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.workflow.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateWfInstanceRequest {

    @NotBlank(message = "processKey 不能为空")
    private String processKey;

    @NotBlank(message = "bizType 不能为空")
    private String bizType;

    @NotNull(message = "bizId 不能为空")
    private Long bizId;

    private Long elderId;

    private Long familyUserId;

    private Long nurseUserId;

    private Long doctorUserId;

    public String getProcessKey() { return processKey; }
    public void setProcessKey(String processKey) { this.processKey = processKey; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Long getFamilyUserId() { return familyUserId; }
    public void setFamilyUserId(Long familyUserId) { this.familyUserId = familyUserId; }
    public Long getNurseUserId() { return nurseUserId; }
    public void setNurseUserId(Long nurseUserId) { this.nurseUserId = nurseUserId; }
    public Long getDoctorUserId() { return doctorUserId; }
    public void setDoctorUserId(Long doctorUserId) { this.doctorUserId = doctorUserId; }
}
