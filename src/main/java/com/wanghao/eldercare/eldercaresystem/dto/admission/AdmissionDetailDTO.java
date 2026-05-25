package com.wanghao.eldercare.eldercaresystem.dto.admission;

import com.fasterxml.jackson.databind.JsonNode;
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdmissionDetailDTO {
    private Long admissionId;
    private Long elderId;
    private Long bedId;
    private String contractNo;
    private String packageName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal depositAmount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long processInstanceId;
    private List<NurseDTO> nurses = new ArrayList<>();
    private BedLocationDTO bed;
    private HealthAssessmentDTO healthAssessment;

    public static AdmissionDetailDTO from(AdmissionRecord record) {
        AdmissionDetailDTO dto = new AdmissionDetailDTO();
        dto.setAdmissionId(record.getAdmissionId());
        dto.setElderId(record.getElderId());
        dto.setBedId(record.getBedId());
        dto.setContractNo(record.getContractNo());
        dto.setPackageName(record.getPackageName());
        dto.setStatus(record.getStatus());
        dto.setStartDate(record.getStartDate());
        dto.setEndDate(record.getEndDate());
        dto.setDepositAmount(record.getDepositAmount());
        dto.setCreatedBy(record.getCreatedBy());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        dto.setProcessInstanceId(record.getProcessInstanceId());
        return dto;
    }

    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(Long processInstanceId) { this.processInstanceId = processInstanceId; }
    public List<NurseDTO> getNurses() { return nurses; }
    public void setNurses(List<NurseDTO> nurses) { this.nurses = nurses; }
    public BedLocationDTO getBed() { return bed; }
    public void setBed(BedLocationDTO bed) { this.bed = bed; }
    public HealthAssessmentDTO getHealthAssessment() { return healthAssessment; }
    public void setHealthAssessment(HealthAssessmentDTO healthAssessment) { this.healthAssessment = healthAssessment; }

    public static class NurseDTO {
        private Long nurseId;
        private String username;
        private String realName;

        public Long getNurseId() { return nurseId; }
        public void setNurseId(Long nurseId) { this.nurseId = nurseId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
    }

    public static class BedLocationDTO {
        private Long bedId;
        private String bedNo;
        private String bedStatus;
        private Long roomId;
        private String roomNo;
        private Long floorId;
        private Integer floorNo;
        private Long buildingId;
        private String buildingName;

        public Long getBedId() { return bedId; }
        public void setBedId(Long bedId) { this.bedId = bedId; }
        public String getBedNo() { return bedNo; }
        public void setBedNo(String bedNo) { this.bedNo = bedNo; }
        public String getBedStatus() { return bedStatus; }
        public void setBedStatus(String bedStatus) { this.bedStatus = bedStatus; }
        public Long getRoomId() { return roomId; }
        public void setRoomId(Long roomId) { this.roomId = roomId; }
        public String getRoomNo() { return roomNo; }
        public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
        public Long getFloorId() { return floorId; }
        public void setFloorId(Long floorId) { this.floorId = floorId; }
        public Integer getFloorNo() { return floorNo; }
        public void setFloorNo(Integer floorNo) { this.floorNo = floorNo; }
        public Long getBuildingId() { return buildingId; }
        public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }
        public String getBuildingName() { return buildingName; }
        public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    }

    public static class HealthAssessmentDTO {
        private Long taskId;
        private String status;
        private Long assessorId;
        private String assessorUsername;
        private String assessorName;
        private LocalDateTime completedAt;
        private String comment;
        private JsonNode formData;
        private ProfileSnapshotDTO profile;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getAssessorId() { return assessorId; }
        public void setAssessorId(Long assessorId) { this.assessorId = assessorId; }
        public String getAssessorUsername() { return assessorUsername; }
        public void setAssessorUsername(String assessorUsername) { this.assessorUsername = assessorUsername; }
        public String getAssessorName() { return assessorName; }
        public void setAssessorName(String assessorName) { this.assessorName = assessorName; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public JsonNode getFormData() { return formData; }
        public void setFormData(JsonNode formData) { this.formData = formData; }
        public ProfileSnapshotDTO getProfile() { return profile; }
        public void setProfile(ProfileSnapshotDTO profile) { this.profile = profile; }
    }

    public static class ProfileSnapshotDTO {
        private String gender;
        private LocalDate birthday;
        private String address;
        private String emergencyContactName;
        private String emergencyContactPhone;
        private String allergies;
        private String chronicConditions;
        private String dietTaboo;
        private String careLevel;
        private String notes;

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public LocalDate getBirthday() { return birthday; }
        public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getEmergencyContactName() { return emergencyContactName; }
        public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
        public String getEmergencyContactPhone() { return emergencyContactPhone; }
        public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
        public String getAllergies() { return allergies; }
        public void setAllergies(String allergies) { this.allergies = allergies; }
        public String getChronicConditions() { return chronicConditions; }
        public void setChronicConditions(String chronicConditions) { this.chronicConditions = chronicConditions; }
        public String getDietTaboo() { return dietTaboo; }
        public void setDietTaboo(String dietTaboo) { this.dietTaboo = dietTaboo; }
        public String getCareLevel() { return careLevel; }
        public void setCareLevel(String careLevel) { this.careLevel = careLevel; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
