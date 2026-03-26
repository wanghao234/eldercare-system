package com.wanghao.eldercare.eldercaresystem.service.auth;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.JwtTokenProvider;
import com.wanghao.eldercare.eldercaresystem.common.security.UserPrincipal;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.PermissionPointService;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.auth.*;
import com.wanghao.eldercare.eldercaresystem.dto.auth.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionPointService permissionPointService;
    private final boolean blockFamilyElderLogin;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       PermissionPointService permissionPointService,
                       @Value("${security.auth.block-family-elder-login:true}") boolean blockFamilyElderLogin) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.permissionPointService = permissionPointService;
        this.blockFamilyElderLogin = blockFamilyElderLogin;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(request.getUsername())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AUTH_INVALID_CREDENTIALS,
                        "用户名或密码错误",
                        HttpStatus.UNAUTHORIZED
                ));

        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED, "账号已禁用", HttpStatus.FORBIDDEN);
        }

        String storedHash = normalizeBcryptPrefix(user.getPasswordHash());
        if (!passwordEncoder.matches(request.getPassword(), storedHash)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }

        if (blockFamilyElderLogin && !isAllowedBackendRole(user.getRole())) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "当前角色不允许登录后台", HttpStatus.FORBIDDEN);
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        var permissions = permissionPointService.loadPermissions(user.getUserId(), user.getRole());
        String token = jwtTokenProvider.createToken(
                user.getUserId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                permissions
        );

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresInSeconds(jwtTokenProvider.getExpirationSeconds());
        response.setUser(UserDTO.from(user));
        return response;
    }

    private String normalizeBcryptPrefix(String hash) {
        if (hash == null) {
            return null;
        }
        if (hash.startsWith("$2y$")) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }

    private boolean isAllowedBackendRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.toLowerCase(Locale.ROOT);
        return !"family".equals(normalized) && !"elder".equals(normalized);
    }

    @Transactional(readOnly = true)
    public UserDTO me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "未认证", HttpStatus.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "未认证", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUserIdAndDeletedAtIsNull(userPrincipal.getUserId())
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        return UserDTO.from(user);
    }
}
