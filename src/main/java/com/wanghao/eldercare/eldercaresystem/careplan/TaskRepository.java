package com.wanghao.eldercare.eldercaresystem.careplan;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByRelatedBizTypeAndRelatedBizId(String relatedBizType, Long relatedBizId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
            delete from Task t
            where t.relatedBizType = :relatedBizType
              and t.relatedBizId = :relatedBizId
              and t.status in :statuses
              and t.dueAt is not null
              and t.dueAt >= :now
            """)
    int deleteFutureByBizAndStatuses(@Param("relatedBizType") String relatedBizType,
                                     @Param("relatedBizId") Long relatedBizId,
                                     @Param("statuses") List<String> statuses,
                                     @Param("now") LocalDateTime now);
}
