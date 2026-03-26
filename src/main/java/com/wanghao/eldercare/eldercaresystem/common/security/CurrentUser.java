package com.wanghao.eldercare.eldercaresystem.common.security;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;

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
        if (targetRole == null) {
            return false;
        }
        String normalizedTargetRole = RoleMapper.normalizeRole(targetRole);
        if (normalizedTargetRole.equals(role)) {
            return true;
        }
        return "admin".equals(normalizedTargetRole) && "nurse_leader".equals(role);
    }
}
