package com.wanghao.eldercare.eldercaresystem.rectification;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RectificationRepository extends JpaRepository<Rectification, Long>, JpaSpecificationExecutor<Rectification> {

    boolean existsBySourceTypeAndSourceId(String sourceType, Long sourceId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Rectification r set r.status = :toStatus, r.updatedAt = :updatedAt where r.rectificationId = :id and r.status = :fromStatus")
    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("fromStatus") String fromStatus,
                            @Param("toStatus") String toStatus,
                            @Param("updatedAt") LocalDateTime updatedAt);
}
