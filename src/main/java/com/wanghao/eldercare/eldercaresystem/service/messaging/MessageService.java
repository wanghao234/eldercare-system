package com.wanghao.eldercare.eldercaresystem.service.messaging;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.common.security.RoleMapper;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.MessageNotificationPublisher;
import com.wanghao.eldercare.eldercaresystem.controller.messaging.*;
import com.wanghao.eldercare.eldercaresystem.dto.messaging.*;
import com.wanghao.eldercare.eldercaresystem.entity.messaging.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.messaging.*;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.notification.NotificationService;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private static final Set<String> ADMIN_ROLES = Set.of("admin", "nurse_leader");

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final CareTeamAssignmentRepository assignmentRepository;
    private final NotificationService notificationService;
    private final MessageNotificationPublisher wsPublisher;

    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          PermissionService permissionService,
                          CareTeamAssignmentRepository assignmentRepository,
                          NotificationService notificationService,
                          MessageNotificationPublisher wsPublisher) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
        this.wsPublisher = wsPublisher;
    }

    @Transactional(readOnly = true)
    public MessageListResponse listConversation(CurrentUser currentUser, Long peerId, Long elderId, int page, int size) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(currentUser, elderId);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Message> result = messageRepository.findConversation(currentUser.getUserId(), peerId, elderId, pageable);

        MessageListResponse response = new MessageListResponse();
        response.setContent(result.getContent().stream().map(MessageDTO::from).toList());
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional
    public MessageDTO send(CurrentUser currentUser, SendMessageRequest request) {
        if (currentUser.getUserId().equals(request.getReceiverId())) {
            throw new AccessDeniedException("不能给自己发送消息");
        }

        User receiver = userRepository.findByUserIdAndDeletedAtIsNull(request.getReceiverId())
                .orElseThrow(() -> new NotFoundException("接收用户不存在"));
        String receiverRole = normalizeRole(receiver.getRole());
        String senderRole = normalizeRole(currentUser.getRole());

        assertCanSend(currentUser, senderRole, receiverRole, receiver.getUserId(), request.getElderId());

        Message m = new Message();
        m.setElderId(request.getElderId());
        m.setSenderId(currentUser.getUserId());
        m.setReceiverId(receiver.getUserId());
        m.setContentType(normalizeContentType(request.getContentType()));
        m.setContent(request.getContent());
        m.setIsRead(0);
        m.setCreatedAt(LocalDateTime.now());
        Message saved = messageRepository.save(m);

        wsPublisher.publishMessageNew(receiver.getUsername(), saved);
        String preview = request.getContent().length() > 100 ? request.getContent().substring(0, 100) : request.getContent();
        notificationService.createMessageNotification(receiver.getUserId(), receiver.getUsername(), saved.getMessageId(), preview);
        return MessageDTO.from(saved);
    }

    @Transactional
    public void markRead(CurrentUser currentUser, Long messageId) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("消息不存在"));
        if (!m.getReceiverId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("无权限标记该消息");
        }
        if (m.getIsRead() != null && m.getIsRead() == 1) {
            return;
        }
        messageRepository.markRead(messageId, currentUser.getUserId());
    }

    private void assertCanSend(CurrentUser currentUser,
                               String senderRole,
                               String receiverRole,
                               Long receiverId,
                               Long elderId) {
        if (isAdminOrLeader(senderRole)) {
            if (elderId != null) {
                permissionService.assertCanAccessElder(currentUser, elderId);
            }
            return;
        }

        if ("family".equals(senderRole)) {
            if (elderId == null) {
                throw new AccessDeniedException("family 发送消息必须提供 elderId 上下文");
            }
            permissionService.assertCanAccessElder(currentUser, elderId);
            if (!("nurse".equals(receiverRole) || "caregiver".equals(receiverRole) || isAdminOrLeader(receiverRole))) {
                throw new AccessDeniedException("family 仅可发送给护理人员或管理员/护士长");
            }
            if (("nurse".equals(receiverRole) || "caregiver".equals(receiverRole))
                    && !assignmentRepository.existsActiveByElderIdAndNurseId(elderId, receiverId)) {
                throw new AccessDeniedException("接收方与老人未建立有效护理绑定关系");
            }
            return;
        }

        if ("nurse".equals(senderRole) || "caregiver".equals(senderRole)) {
            if (elderId == null) {
                throw new AccessDeniedException("护理人员发送消息必须提供 elderId 上下文");
            }
            permissionService.assertCanAccessElder(currentUser, elderId);
            if ("family".equals(receiverRole)) {
                if (!assignmentRepository.existsActiveByElderIdAndFamilyId(elderId, receiverId)) {
                    throw new AccessDeniedException("接收家属与老人未建立有效绑定关系");
                }
                return;
            }
            if (isAdminOrLeader(receiverRole)) {
                return;
            }
            throw new AccessDeniedException("护理人员仅可发送给绑定家属或管理员/护士长");
        }

        throw new AccessDeniedException("当前角色不允许发送消息");
    }

    private boolean isAdminOrLeader(String role) {
        return ADMIN_ROLES.contains(role);
    }

    private String normalizeContentType(String type) {
        String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (!"text".equals(t) && !"image".equals(t) && !"file".equals(t)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "contentType 仅支持 text/image/file", HttpStatus.BAD_REQUEST);
        }
        return t;
    }

    private String normalizeRole(String role) {
        return RoleMapper.normalizeRole(role);
    }
}
