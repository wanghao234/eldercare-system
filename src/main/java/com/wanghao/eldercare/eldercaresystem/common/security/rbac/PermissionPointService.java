package com.wanghao.eldercare.eldercaresystem.common.security.rbac;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.RoleMapper;
import com.wanghao.eldercare.eldercaresystem.common.security.UserPrincipal;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PermissionPointService {

    private static final Logger log = LoggerFactory.getLogger(PermissionPointService.class);

    private static final String PERM_QUERY = """
            select distinct p.perm_code
            from permissions p
            join role_permission rp on rp.perm_id = p.perm_id
            join user_role ur on ur.role_id = rp.role_id
            where ur.user_id = ?
            """;

    private static final String LEGACY_ROLE_PERM_QUERY = """
            select distinct p.perm_code
            from permissions p
            join role_permission rp on rp.perm_id = p.perm_id
            join roles r on r.role_id = rp.role_id
            where lower(r.role_code) = lower(?)
            """;

    private static final Map<String, Set<String>> LEGACY_DEFAULT_PERMS = Map.of(
            "admin", Set.of(
                    "alarm:read", "alarm:handle",
                    "rectification:read", "rectification:handle",
                    "qc:manage", "audit:read", "medication:manage"
            ),
            "nurse_leader", Set.of(
                    "alarm:read", "alarm:handle",
                    "rectification:read", "rectification:handle",
                    "qc:manage", "audit:read", "medication:manage"
            ),
            "nurse", Set.of("alarm:read", "alarm:handle", "rectification:read", "rectification:handle", "audit:read"),
            "caregiver", Set.of("alarm:read", "alarm:handle", "rectification:read", "rectification:handle", "audit:read"),
            "doctor", Set.of("alarm:read", "rectification:read", "audit:read")
    );

    private final JdbcTemplate jdbcTemplate;

    public PermissionPointService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<String> loadPermissions(Long userId, String role) {
        LinkedHashSet<String> perms = new LinkedHashSet<>();
        String normalizedRole = RoleMapper.normalizeRole(role).toLowerCase(Locale.ROOT);
        try {
            if (userId != null) {
                perms.addAll(jdbcTemplate.query(PERM_QUERY, (rs, rowNum) -> rs.getString(1), userId));
            }
            if (!normalizedRole.isBlank()) {
                perms.addAll(jdbcTemplate.query(LEGACY_ROLE_PERM_QUERY, (rs, rowNum) -> rs.getString(1), normalizedRole));
            }
        } catch (DataAccessException ex) {
            log.warn("Load RBAC perms failed, fallback to compatibility defaults: {}", ex.getMessage());
        }
        perms.addAll(LEGACY_DEFAULT_PERMS.getOrDefault(normalizedRole, Collections.emptySet()));
        return perms;
    }

    public boolean hasPermission(UserPrincipal principal, String permCode) {
        if (principal == null || permCode == null || permCode.isBlank()) {
            return false;
        }
        if (principal.hasPermission(permCode)) {
            return true;
        }
        String normalizedRole = RoleMapper.normalizeRole(principal.getRole()).toLowerCase(Locale.ROOT);
        return LEGACY_DEFAULT_PERMS.getOrDefault(normalizedRole, Collections.emptySet()).contains(permCode);
    }
}
