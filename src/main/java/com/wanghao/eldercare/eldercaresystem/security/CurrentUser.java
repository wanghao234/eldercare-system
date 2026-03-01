package com.wanghao.eldercare.eldercaresystem.security;

public class CurrentUser {
    private final Long userId;
    private final String username;
    private final String role;

    public CurrentUser(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = RoleMapper.normalizeRole(role);
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean hasRole(String targetRole) {
        return targetRole != null && RoleMapper.normalizeRole(targetRole).equals(role);
    }
}
