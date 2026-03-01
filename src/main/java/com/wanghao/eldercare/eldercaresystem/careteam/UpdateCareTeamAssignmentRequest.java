package com.wanghao.eldercare.eldercaresystem.careteam;

public class UpdateCareTeamAssignmentRequest {
    private Long elderId;
    private Long nurseId;
    private Long familyId;
    private Integer isActive;
    private Boolean unbindNurse;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getNurseId() {
        return nurseId;
    }

    public void setNurseId(Long nurseId) {
        this.nurseId = nurseId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public Boolean getUnbindNurse() {
        return unbindNurse;
    }

    public void setUnbindNurse(Boolean unbindNurse) {
        this.unbindNurse = unbindNurse;
    }
}
