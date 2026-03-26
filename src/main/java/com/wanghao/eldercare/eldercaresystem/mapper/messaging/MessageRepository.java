package com.wanghao.eldercare.eldercaresystem.mapper.messaging;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.messaging.*;
import com.wanghao.eldercare.eldercaresystem.dto.messaging.*;
import com.wanghao.eldercare.eldercaresystem.entity.messaging.*;
import com.wanghao.eldercare.eldercaresystem.service.messaging.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            select m from Message m
            where ((m.senderId = :userId and m.receiverId = :peerId) or (m.senderId = :peerId and m.receiverId = :userId))
              and (:elderId is null or m.elderId = :elderId)
            """)
    Page<Message> findConversation(@Param("userId") Long userId,
                                   @Param("peerId") Long peerId,
                                   @Param("elderId") Long elderId,
                                   Pageable pageable);

    Page<Message> findByReceiverIdAndIsRead(Long receiverId, Integer isRead, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Message m set m.isRead = 1 where m.messageId = :messageId and m.receiverId = :receiverId and m.isRead = 0")
    int markRead(@Param("messageId") Long messageId, @Param("receiverId") Long receiverId);
}
