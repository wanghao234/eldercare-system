package com.wanghao.eldercare.eldercaresystem.service.notification;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.MessageNotificationPublisher;
import com.wanghao.eldercare.eldercaresystem.controller.notification.*;
import com.wanghao.eldercare.eldercaresystem.dto.notification.*;
import com.wanghao.eldercare.eldercaresystem.entity.notification.*;
import com.wanghao.eldercare.eldercaresystem.mapper.notification.*;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MessageNotificationPublisher publisher;

    public NotificationService(NotificationRepository notificationRepository,
                               MessageNotificationPublisher publisher) {
        this.notificationRepository = notificationRepository;
        this.publisher = publisher;
    }

    @Transactional
    public Notification createMessageNotification(Long toUserId,
                                                  String toUsername,
                                                  Long messageId,
                                                  String contentPreview) {
        Notification n = new Notification();
        n.setToUserId(toUserId);
        n.setTitle("新消息");
        n.setContent(contentPreview);
        n.setNotifType("message");
        n.setBizType("message");
        n.setBizId(messageId);
        n.setIsRead(0);
        n.setCreatedAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(n);
        publisher.publishNotificationNew(toUsername, saved);
        return saved;
    }

    @Transactional
    public Notification createSystemNotification(Long toUserId,
                                                 String toUsername,
                                                 String title,
                                                 String content,
                                                 String notifType,
                                                 String bizType,
                                                 Long bizId) {
        Notification n = new Notification();
        n.setToUserId(toUserId);
        n.setTitle(title);
        n.setContent(content);
        n.setNotifType(notifType);
        n.setBizType(bizType);
        n.setBizId(bizId);
        n.setIsRead(0);
        n.setCreatedAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(n);
        publisher.publishNotificationNew(toUsername, saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse listMy(CurrentUser currentUser, Integer isRead, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = isRead == null
                ? notificationRepository.findByToUserId(currentUser.getUserId(), pageable)
                : notificationRepository.findByToUserIdAndIsRead(currentUser.getUserId(), isRead, pageable);

        NotificationListResponse response = new NotificationListResponse();
        response.setContent(result.getContent().stream().map(NotificationDTO::from).toList());
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional
    public void markRead(CurrentUser currentUser, Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("通知不存在"));
        if (!notification.getToUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("无权限标记该通知");
        }
        if (notification.getIsRead() != null && notification.getIsRead() == 1) {
            return;
        }
        notificationRepository.markRead(id, currentUser.getUserId(), LocalDateTime.now());
    }
}
