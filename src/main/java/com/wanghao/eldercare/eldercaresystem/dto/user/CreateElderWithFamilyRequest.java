package com.wanghao.eldercare.eldercaresystem.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateElderWithFamilyRequest {

    @NotBlank(message = "username 不能为空")
    @Size(max = 64, message = "username 长度不能超过64")
    private String username;

    @NotBlank(message = "password 不能为空")
    @Size(min = 6, max = 128, message = "password 长度需在6-128之间")
    private String password;

    @NotBlank(message = "realName 不能为空")
    @Size(max = 64, message = "realName 长度不能超过64")
    private String realName;

    @NotBlank(message = "phone 不能为空")
    @Size(max = 32, message = "phone 长度不能超过32")
    private String phone;

    @NotBlank(message = "familyName 不能为空")
    @Size(max = 64, message = "familyName 长度不能超过64")
    private String familyName;

    @NotBlank(message = "familyPhone 不能为空")
    @Size(max = 32, message = "familyPhone 长度不能超过32")
    private String familyPhone;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getFamilyPhone() {
        return familyPhone;
    }

    public void setFamilyPhone(String familyPhone) {
        this.familyPhone = familyPhone;
    }
}
