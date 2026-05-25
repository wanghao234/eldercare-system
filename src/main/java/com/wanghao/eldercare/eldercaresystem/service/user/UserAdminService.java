package com.wanghao.eldercare.eldercaresystem.service.user;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.user.*;
import com.wanghao.eldercare.eldercaresystem.dto.user.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.profile.ElderProfileEntity;
import com.wanghao.eldercare.eldercaresystem.entity.user.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.ElderProfileRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.*;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "admin", "nurse_leader", "nurse", "caregiver", "doctor", "family", "elder"
    );
    private static final Set<String> ALLOWED_STATUS = Set.of("active", "disabled");
    private static final String DEFAULT_FAMILY_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final CareTeamAssignmentRepository careTeamAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserRepository userRepository,
                            ElderProfileRepository elderProfileRepository,
                            CareTeamAssignmentRepository careTeamAssignmentRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.careTeamAssignmentRepository = careTeamAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserAdminPageResponse list(String keyword, String role, String status, int page, int size) {
        String normalizedKeyword = normalizeOptionalText(keyword);
        String normalizedRole = normalizeOptionalRole(role);
        String normalizedStatus = normalizeOptionalStatus(status);
        Page<User> result = userRepository.search(
                normalizedKeyword,
                normalizedRole,
                normalizedStatus,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "userId"))
        );

        UserAdminPageResponse response = new UserAdminPageResponse();
        response.setContent(result.getContent().stream().map(UserAdminDTO::from).toList());
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public UserAdminDTO getById(Long userId) {
        return UserAdminDTO.from(findByIdOrThrow(userId));
    }

    @Transactional
    public UserAdminDTO create(CreateUserRequest request) {
        String username = normalizeRequiredText(request.getUsername(), "username 不能为空");
        assertUsernameAvailable(username);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(normalizeRequiredRole(request.getRole()));
        user.setStatus(normalizeStatusOrDefault(request.getStatus(), "active"));
        user.setRealName(normalizeOptionalText(request.getRealName()));
        user.setPhone(normalizeOptionalText(request.getPhone()));
        user.setEmail(normalizeOptionalText(request.getEmail()));
        user.setAvatarUrl(normalizeOptionalText(request.getAvatarUrl()));
        user.setLastLoginAt(null);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeletedAt(null);

        return UserAdminDTO.from(userRepository.save(user));
    }

    @Transactional
    public ElderWithFamilyCreateResponse createElderWithFamily(CreateElderWithFamilyRequest request) {
        String elderUsername = normalizeRequiredText(request.getUsername(), "username 不能为空");
        String elderPassword = normalizeRequiredText(request.getPassword(), "password 不能为空");
        String elderRealName = normalizeRequiredText(request.getRealName(), "realName 不能为空");
        String elderPhone = normalizeRequiredText(request.getPhone(), "phone 不能为空");
        String familyRealName = normalizeRequiredText(request.getFamilyName(), "familyName 不能为空");
        String familyPhone = normalizeRequiredText(request.getFamilyPhone(), "familyPhone 不能为空");
        String familyUsername = familyPhone;

        assertUsernameAvailable(elderUsername);
        assertUsernameAvailable(familyUsername);

        LocalDateTime now = LocalDateTime.now();

        User elder = new User();
        elder.setUsername(elderUsername);
        elder.setPasswordHash(passwordEncoder.encode(elderPassword));
        elder.setRole("elder");
        elder.setStatus("active");
        elder.setRealName(elderRealName);
        elder.setPhone(elderPhone);
        elder.setEmail(null);
        elder.setAvatarUrl(null);
        elder.setLastLoginAt(null);
        elder.setCreatedAt(now);
        elder.setUpdatedAt(now);
        elder.setDeletedAt(null);
        elder = userRepository.saveAndFlush(elder);

        User family = new User();
        family.setUsername(familyUsername);
        family.setPasswordHash(passwordEncoder.encode(DEFAULT_FAMILY_PASSWORD));
        family.setRole("family");
        family.setStatus("active");
        family.setRealName(familyRealName);
        family.setPhone(familyPhone);
        family.setEmail(null);
        family.setAvatarUrl(null);
        family.setLastLoginAt(null);
        family.setCreatedAt(now);
        family.setUpdatedAt(now);
        family.setDeletedAt(null);
        family = userRepository.saveAndFlush(family);

        ElderProfileEntity elderProfile = new ElderProfileEntity();
        elderProfile.setElderId(elder.getUserId());
        elderProfile.setEmergencyContactName(familyRealName);
        elderProfile.setEmergencyContactPhone(familyPhone);
        elderProfile.setCreatedAt(now);
        elderProfile.setUpdatedAt(now);
        elderProfileRepository.saveAndFlush(elderProfile);

        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elder.getUserId());
        assignment.setFamilyId(family.getUserId());
        assignment.setIsActive(1);
        assignment.setCreatedAt(now);
        assignment.setUpdatedAt(now);
        careTeamAssignmentRepository.saveAndFlush(assignment);

        ElderWithFamilyCreateResponse response = new ElderWithFamilyCreateResponse();
        response.setElder(UserAdminDTO.from(elder));
        response.setFamily(UserAdminDTO.from(family));
        return response;
    }

    @Transactional
    public UserAdminDTO update(Long userId, UpdateUserRequest request) {
        User user = findByIdOrThrow(userId);

        String username = normalizeOptionalText(request.getUsername());
        if (username != null && !username.equals(user.getUsername())) {
            if (userRepository.existsByUsernameAndUserIdNot(username, userId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在", HttpStatus.BAD_REQUEST);
            }
            user.setUsername(username);
        }

        String password = normalizeOptionalText(request.getPassword());
        if (password != null) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        String role = normalizeOptionalRole(request.getRole());
        if (role != null) {
            user.setRole(role);
        }

        String status = normalizeOptionalStatus(request.getStatus());
        if (status != null) {
            user.setStatus(status);
        }

        if (request.getRealName() != null) {
            user.setRealName(normalizeOptionalText(request.getRealName()));
        }
        if (request.getPhone() != null) {
            user.setPhone(normalizeOptionalText(request.getPhone()));
        }
        if (request.getEmail() != null) {
            user.setEmail(normalizeOptionalText(request.getEmail()));
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(normalizeOptionalText(request.getAvatarUrl()));
        }
        if (request.getLastLoginAt() != null) {
            user.setLastLoginAt(request.getLastLoginAt());
        }

        user.setUpdatedAt(LocalDateTime.now());
        return UserAdminDTO.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long userId) {
        int updated = userRepository.softDeleteById(userId, LocalDateTime.now());
        if (updated == 0) {
            throw new NotFoundException("用户不存在");
        }
    }

    private User findByIdOrThrow(Long userId) {
        return userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
    }

    private void assertUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeRequiredText(String value, String emptyMessage) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, emptyMessage, HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequiredRole(String role) {
        String normalized = normalizeOptionalRole(role);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "role 不能为空", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeOptionalRole(String role) {
        String normalized = normalizeOptionalText(role);
        if (normalized == null) {
            return null;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(lower)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "role 不合法", HttpStatus.BAD_REQUEST);
        }
        return lower;
    }

    private String normalizeStatusOrDefault(String status, String defaultValue) {
        String normalized = normalizeOptionalStatus(status);
        return normalized == null ? defaultValue : normalized;
    }

    private String normalizeOptionalStatus(String status) {
        String normalized = normalizeOptionalText(status);
        if (normalized == null) {
            return null;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_STATUS.contains(lower)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 不合法", HttpStatus.BAD_REQUEST);
        }
        return lower;
    }
}
