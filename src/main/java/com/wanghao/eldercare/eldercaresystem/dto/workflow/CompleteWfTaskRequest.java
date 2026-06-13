package com.wanghao.eldercare.eldercaresystem.dto.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonAlias;
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
import java.util.List;

public class CompleteWfTaskRequest {

    private String action;

    private String comment;

    private Boolean approved;

    private JsonNode formData;

    private String formDataJson;

    private Long elderId;

    private Long nurseId;

    private List<Long> nurseIds;

    private Long familyId;

    private List<Long> familyIds;

    @JsonAlias({"attachmentsJson"})
    private List<String> attachments;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public JsonNode getFormData() { return formData; }
    public void setFormData(JsonNode formData) { this.formData = formData; }
    public String getFormDataJson() { return formDataJson; }
    public void setFormDataJson(String formDataJson) { this.formDataJson = formDataJson; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Long getNurseId() { return nurseId; }
    public void setNurseId(Long nurseId) { this.nurseId = nurseId; }
    public List<Long> getNurseIds() { return nurseIds; }
    public void setNurseIds(List<Long> nurseIds) { this.nurseIds = nurseIds; }
    public Long getFamilyId() { return familyId; }
    public void setFamilyId(Long familyId) { this.familyId = familyId; }
    public List<Long> getFamilyIds() { return familyIds; }
    public void setFamilyIds(List<Long> familyIds) { this.familyIds = familyIds; }
    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }
}
