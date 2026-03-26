package com.wanghao.eldercare.eldercaresystem.dto.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.profile.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.profile.*;
import com.wanghao.eldercare.eldercaresystem.entity.profile.*;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.*;
import com.wanghao.eldercare.eldercaresystem.service.profile.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class ElderProfileUpdateRequest {

    @JsonAlias({"name", "real_name"})
    @Size(max = 64, message = "realName 长度不能超过64")
    private String realName;

    @Pattern(regexp = "male|female|unknown", message = "gender 仅支持 male/female/unknown")
    private String gender;

    @JsonAlias({"birthDate", "birth_date"})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @JsonAlias({"idNo", "id_no", "id_number", "identityNo", "identity_no"})
    @Size(max = 64, message = "idNumber 长度不能超过64")
    private String idNumber;

    @JsonAlias({"homeAddress", "home_address"})
    @Size(max = 255, message = "address 长度不能超过255")
    private String address;

    @JsonAlias({"emergencyName", "emergency_name", "emergency_contact_name"})
    @Size(max = 64, message = "emergencyContactName 长度不能超过64")
    private String emergencyContactName;

    @JsonAlias({"emergencyPhone", "emergency_phone", "emergency_contact_phone"})
    @Size(max = 32, message = "emergencyContactPhone 长度不能超过32")
    private String emergencyContactPhone;

    @JsonAlias({"allergy", "allergyHistory", "allergy_history"})
    @Size(max = 500, message = "allergies 长度不能超过500")
    private String allergies;

    @JsonAlias({"chronic", "chronic_conditions", "medicalHistory", "medical_history"})
    @Size(max = 500, message = "chronicConditions 长度不能超过500")
    private String chronicConditions;

    @JsonAlias({"dietTaboos", "diet_taboos", "diet_taboo"})
    @Size(max = 500, message = "dietTaboo 长度不能超过500")
    private String dietTaboo;

    @JsonAlias({"care_level", "level"})
    @Size(max = 32, message = "careLevel 长度不能超过32")
    private String careLevel;

    @Size(max = 1000, message = "notes 长度不能超过1000")
    private String notes;

    @Size(max = 32, message = "phone 长度不能超过32")
    private String phone;

    @Size(max = 128, message = "email 长度不能超过128")
    private String email;

    @JsonAlias({"avatar", "avatar_url"})
    @Size(max = 255, message = "avatarUrl 长度不能超过255")
    private String avatarUrl;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
