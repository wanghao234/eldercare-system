package com.wanghao.eldercare.eldercaresystem.notification;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByToUserId(Long toUserId, Pageable pageable);

    Page<Notification> findByToUserIdAndIsRead(Long toUserId, Integer isRead, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Notification n set n.isRead = 1, n.readAt = :readAt where n.notificationId = :id and n.toUserId = :toUserId and n.isRead = 0")
    int markRead(@Param("id") Long id, @Param("toUserId") Long toUserId, @Param("readAt") java.time.LocalDateTime readAt);
}
