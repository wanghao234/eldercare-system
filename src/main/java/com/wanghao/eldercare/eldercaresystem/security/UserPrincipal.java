package com.wanghao.eldercare.eldercaresystem.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UserPrincipal implements UserDetails {
    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final String role;
    private final String status;
    private final Set<String> permissions;

    public UserPrincipal(Long userId, String username, String passwordHash, String role, String status) {
        this(userId, username, passwordHash, role, status, Set.of());
    }

    public UserPrincipal(Long userId,
                         String username,
                         String passwordHash,
                         String role,
                         String status,
                         Set<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(String permCode) {
        return permissions.contains(permCode);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority(RoleMapper.toAuthority(role)));
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return List.copyOf(authorities);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "active".equalsIgnoreCase(status);
    }
}
