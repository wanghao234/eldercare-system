package com.wanghao.eldercare.eldercaresystem.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "elder_profile")
public class ElderProfileEntity {

    @Id
    @Column(name = "elder_id")
    private Long elderId;

    @Column(name = "gender", length = 16)
    private String gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "id_number", length = 64)
    private String idNumber;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "emergency_contact_name", length = 64)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 32)
    private String emergencyContactPhone;

    @Column(name = "allergies", length = 500)
    private String allergies;

    @Column(name = "chronic_conditions", length = 500)
    private String chronicConditions;

    @Column(name = "diet_taboo", length = 500)
    private String dietTaboo;

    @Column(name = "care_level", length = 32)
    private String careLevel;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getChronicConditions() {
        return chronicConditions;
    }

    public void setChronicConditions(String chronicConditions) {
        this.chronicConditions = chronicConditions;
    }

    public String getDietTaboo() {
        return dietTaboo;
    }

    public void setDietTaboo(String dietTaboo) {
        this.dietTaboo = dietTaboo;
    }

    public String getCareLevel() {
        return careLevel;
    }

    public void setCareLevel(String careLevel) {
        this.careLevel = careLevel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
