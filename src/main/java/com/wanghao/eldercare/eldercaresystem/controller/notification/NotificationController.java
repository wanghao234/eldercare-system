package com.wanghao.eldercare.eldercaresystem.controller.notification;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.notification.*;
import com.wanghao.eldercare.eldercaresystem.entity.notification.*;
import com.wanghao.eldercare.eldercaresystem.mapper.notification.*;
import com.wanghao.eldercare.eldercaresystem.service.notification.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserUtils currentUserUtils;

    public NotificationController(NotificationService notificationService, CurrentUserUtils currentUserUtils) {
        this.notificationService = notificationService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping("/my")
    public ApiResponse<NotificationListResponse> my(@RequestParam(required = false) Integer isRead,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(notificationService.listMy(user, isRead, page, size));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> read(@PathVariable Long id) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        notificationService.markRead(user, id);
        return ApiResponse.ok(null);
    }
}
