package com.wanghao.eldercare.eldercaresystem.controller.messaging;

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
import com.wanghao.eldercare.eldercaresystem.dto.messaging.*;
import com.wanghao.eldercare.eldercaresystem.entity.messaging.*;
import com.wanghao.eldercare.eldercaresystem.mapper.messaging.*;
import com.wanghao.eldercare.eldercaresystem.service.messaging.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserUtils currentUserUtils;

    public MessageController(MessageService messageService, CurrentUserUtils currentUserUtils) {
        this.messageService = messageService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping
    public ApiResponse<MessageListResponse> list(@RequestParam Long peerId,
                                                 @RequestParam(required = false) Long elderId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(messageService.listConversation(user, peerId, elderId, page, size));
    }

    @PostMapping
    public ApiResponse<MessageDTO> send(@Valid @RequestBody SendMessageRequest request) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(messageService.send(user, request));
    }

    @PostMapping("/{messageId}/read")
    public ApiResponse<Void> read(@PathVariable Long messageId) {
        CurrentUser user = currentUserUtils.getCurrentUser();
        messageService.markRead(user, messageId);
        return ApiResponse.ok(null);
    }
}
