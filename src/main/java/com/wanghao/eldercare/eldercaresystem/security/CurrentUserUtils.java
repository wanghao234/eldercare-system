package com.wanghao.eldercare.eldercaresystem.security;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserUtils {

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
}
