package com.wanghao.eldercare.eldercaresystem.notification;

import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
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
