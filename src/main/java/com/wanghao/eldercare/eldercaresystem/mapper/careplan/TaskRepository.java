package com.wanghao.eldercare.eldercaresystem.mapper.careplan;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careplan.*;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.service.careplan.*;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
