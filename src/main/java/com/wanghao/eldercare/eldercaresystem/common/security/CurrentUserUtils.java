package com.wanghao.eldercare.eldercaresystem.common.security;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserUtils {

    private final UserRepository userRepository;

    public CurrentUserUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未认证", HttpStatus.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未认证", HttpStatus.UNAUTHORIZED);
        }

        return new CurrentUser(userPrincipal.getUserId(), userPrincipal.getUsername(), userPrincipal.getRole());
    }

    public CurrentUser getCurrentUserOrSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return resolveSystemCurrentUser();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return new CurrentUser(userPrincipal.getUserId(), userPrincipal.getUsername(), userPrincipal.getRole());
        }
        return resolveSystemCurrentUser();
    }

    private CurrentUser resolveSystemCurrentUser() {
        User systemUser = userRepository.findByUsernameAndDeletedAtIsNull("system")
                .filter(user -> "active".equalsIgnoreCase(user.getStatus()))
                .or(() -> userRepository.findFirstByRoleIgnoreCaseAndStatusIgnoreCaseAndDeletedAtIsNullOrderByUserIdAsc("admin", "active"))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SYSTEM_ERROR,
                        "未找到可用于匿名报警的 system/admin 用户",
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));
        return new CurrentUser(systemUser.getUserId(), systemUser.getUsername(), systemUser.getRole());
    }
}
