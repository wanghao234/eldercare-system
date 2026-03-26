package com.wanghao.eldercare.eldercaresystem.service.elder;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.elder.*;
import com.wanghao.eldercare.eldercaresystem.dto.elder.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElderService {

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    public ElderService(PermissionService permissionService, UserRepository userRepository) {
        this.permissionService = permissionService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ElderProfileDTO getElderProfile(CurrentUser currentUser, Long elderId) {
        permissionService.assertCanAccessElder(currentUser, elderId);

        User elder = userRepository.findByUserIdAndDeletedAtIsNull(elderId)
                .orElseThrow(() -> new NotFoundException("老人不存在"));

        ElderProfileDTO dto = new ElderProfileDTO();
        dto.setElderId(elder.getUserId());
        dto.setRealName(elder.getRealName());
        dto.setPhone(elder.getPhone());
        dto.setAvatarUrl(elder.getAvatarUrl());
        dto.setStatus(elder.getStatus());
        return dto;
    }
}
