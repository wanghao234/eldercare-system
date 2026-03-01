package com.wanghao.eldercare.eldercaresystem.elder;

import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.user.User;
import com.wanghao.eldercare.eldercaresystem.user.UserRepository;
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
