package com.wanghao.eldercare.eldercaresystem.mapper.task;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.task.*;
import com.wanghao.eldercare.eldercaresystem.dto.task.*;
import com.wanghao.eldercare.eldercaresystem.entity.task.*;
import com.wanghao.eldercare.eldercaresystem.service.task.*;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository("unifiedTaskRepository")
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Page<Task> findByAssignedToAndStatusIn(Long assignedTo, List<String> statuses, Pageable pageable);

    List<Task> findByRelatedBizTypeAndRelatedBizId(String relatedBizType, Long relatedBizId);

    List<Task> findByRelatedBizTypeAndRelatedBizIdAndTaskTypeAndStatusIn(String relatedBizType,
                                                                          Long relatedBizId,
                                                                          String taskType,
                                                                          List<String> statuses);

    @Query("select t.status from UnifiedTask t where t.taskId = :taskId")
    Optional<String> findStatusByTaskId(@Param("taskId") Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update UnifiedTask t set t.status = :toStatus, t.updatedAt = :updatedAt where t.taskId = :taskId and t.status in :fromStatuses")
    int updateStatusIfMatch(@Param("taskId") Long taskId,
                            @Param("fromStatuses") List<String> fromStatuses,
                            @Param("toStatus") String toStatus,
                            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update UnifiedTask t set t.status = 'completed', t.completedBy = :completedBy, t.completedAt = :completedAt, t.updatedAt = :updatedAt where t.taskId = :taskId and t.status in :fromStatuses")
    int completeIfMatch(@Param("taskId") Long taskId,
                        @Param("fromStatuses") List<String> fromStatuses,
                        @Param("completedBy") Long completedBy,
                        @Param("completedAt") LocalDateTime completedAt,
                        @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
            update UnifiedTask t
            set t.status = 'overdue', t.updatedAt = :updatedAt
            where t.status in :fromStatuses
              and t.dueAt is not null
              and t.dueAt < :updatedAt
            """)
    int markOverdue(@Param("fromStatuses") List<String> fromStatuses,
                    @Param("updatedAt") LocalDateTime updatedAt);
}
