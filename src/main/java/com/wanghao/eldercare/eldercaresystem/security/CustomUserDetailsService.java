package com.wanghao.eldercare.eldercaresystem.security;

import com.wanghao.eldercare.eldercaresystem.user.User;
import com.wanghao.eldercare.eldercaresystem.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.security.rbac.PermissionPointService;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionPointService permissionPointService;

    public CustomUserDetailsService(UserRepository userRepository, PermissionPointService permissionPointService) {
        this.userRepository = userRepository;
        this.permissionPointService = permissionPointService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new DisabledException("账号已禁用");
        }

        return new UserPrincipal(
                user.getUserId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus(),
                permissionPointService.loadPermissions(user.getUserId(), user.getRole())
        );
    }
}
