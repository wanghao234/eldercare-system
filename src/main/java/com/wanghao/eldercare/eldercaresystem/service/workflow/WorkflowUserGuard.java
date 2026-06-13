package com.wanghao.eldercare.eldercaresystem.service.workflow;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WorkflowUserGuard {

    private final UserRepository userRepository;

    public WorkflowUserGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireActive(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw forbidden("未登录用户不能操作流程");
        }
        User user = userRepository.findByUserIdAndDeletedAtIsNull(currentUser.getUserId())
                .orElseThrow(() -> forbidden("用户不存在或已删除"));
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw forbidden("用户已禁用，不能操作流程");
        }
        return user;
    }

    private BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }
}
