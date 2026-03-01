package com.wanghao.eldercare.eldercaresystem.careteam;

import java.time.LocalDateTime;

public class CareTeamAssignmentDTO {
    private Long assignmentId;
    private Long elderId;
    private Long nurseId;
    private Long familyId;
    private Integer isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CareTeamAssignmentDTO from(CareTeamAssignment entity) {
        CareTeamAssignmentDTO dto = new CareTeamAssignmentDTO();
        dto.setAssignmentId(entity.getAssignmentId());
        dto.setElderId(entity.getElderId());
        dto.setNurseId(entity.getNurseId());
        dto.setFamilyId(entity.getFamilyId());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
