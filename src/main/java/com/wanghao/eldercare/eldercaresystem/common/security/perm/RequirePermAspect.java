package com.wanghao.eldercare.eldercaresystem.common.security.perm;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.UserPrincipal;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.PermissionPointService;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequirePermAspect {

    private final PermissionPointService permissionPointService;

    public RequirePermAspect(PermissionPointService permissionPointService) {
        this.permissionPointService = permissionPointService;
    }

    @Around("@annotation(requirePerm)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, RequirePerm requirePerm) throws Throwable {
        check(requirePerm.value());
        return joinPoint.proceed();
    }

    @Around("@within(requirePerm)")
    public Object aroundType(ProceedingJoinPoint joinPoint, RequirePerm requirePerm) throws Throwable {
        check(requirePerm.value());
        return joinPoint.proceed();
    }

    private void check(String permCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new AccessDeniedException("无权限访问该资源");
        }
        if (!permissionPointService.hasPermission(userPrincipal, permCode)) {
            throw new AccessDeniedException("缺少权限点: " + permCode);
        }
    }
}
