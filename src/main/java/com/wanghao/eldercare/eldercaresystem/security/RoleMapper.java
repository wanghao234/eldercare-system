package com.wanghao.eldercare.eldercaresystem.security;

import java.util.Locale;

public final class RoleMapper {

    private RoleMapper() {
    }

    public static String toAuthority(String role) {
        return switch (normalizeRole(role)) {
            case "admin" -> "ROLE_ADMIN";
            case "nurse_leader" -> "ROLE_NURSE_LEADER";
            case "nurse" -> "ROLE_NURSE";
            case "caregiver" -> "ROLE_CAREGIVER";
            case "family" -> "ROLE_FAMILY";
            case "elder" -> "ROLE_ELDER";
            case "doctor" -> "ROLE_DOCTOR";
            default -> throw new IllegalArgumentException("未知角色: " + role);
        };
    }

    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role 不能为空");
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "护士长", "nurse-leader", "nurseleader" -> "nurse_leader";
            case "护工", "护理员", "care-worker" -> "caregiver";
            case "医生" -> "doctor";
            default -> normalized;
        };
    }
}
