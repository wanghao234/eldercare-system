package com.wanghao.eldercare.eldercaresystem.profile.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class StaffProfileUpdateRequest {

    @Size(max = 64, message = "jobTitle 长度不能超过64")
    private String jobTitle;

    @Size(max = 64, message = "department 长度不能超过64")
    private String department;

    @Size(max = 128, message = "certificationNo 长度不能超过128")
    private String certificationNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    private List<@Size(max = 64, message = "skills 单项长度不能超过64") String> skills;

    @Size(max = 32, message = "phone 长度不能超过32")
    private String phone;

    @Size(max = 255, message = "avatarUrl 长度不能超过255")
    private String avatarUrl;

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCertificationNo() {
        return certificationNo;
    }

    public void setCertificationNo(String certificationNo) {
        this.certificationNo = certificationNo;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
