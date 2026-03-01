package com.wanghao.eldercare.eldercaresystem.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {
    @NotBlank(message = "username 不能为空")
    @Size(max = 64, message = "username 长度不能超过64")
    private String username;

    @NotBlank(message = "password 不能为空")
    @Size(min = 6, max = 128, message = "password 长度需在6-128之间")
    private String password;

    @NotBlank(message = "role 不能为空")
    @Size(max = 32, message = "role 长度不能超过32")
    private String role;

    @Size(max = 16, message = "status 长度不能超过16")
    private String status;

    @Size(max = 64, message = "realName 长度不能超过64")
    private String realName;

    @Size(max = 32, message = "phone 长度不能超过32")
    private String phone;

    @Size(max = 128, message = "email 长度不能超过128")
    private String email;

    @Size(max = 255, message = "avatarUrl 长度不能超过255")
    private String avatarUrl;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
