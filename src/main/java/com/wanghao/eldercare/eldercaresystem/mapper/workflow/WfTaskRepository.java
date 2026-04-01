package com.wanghao.eldercare.eldercaresystem.mapper.workflow;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.workflow.*;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.*;
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.workflow.*;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WfTaskRepository extends JpaRepository<WfTask, Long> {

    @Query("select t from WfTask t where (t.assigneeId = :userId or t.candidateRole = :role) and t.status in :statuses")
    Page<WfTask> findMyTodo(@Param("userId") Long userId,
                            @Param("role") String role,
                            @Param("statuses") Collection<String> statuses,
                            Pageable pageable);

    @Query("""
            select distinct t
            from WfTask t, WfInstance i, AdmissionRecord a, CareTeamAssignment c
            where t.instanceId = i.instanceId
              and i.bizType = 'admission'
              and i.bizId = a.admissionId
              and a.elderId = c.elderId
              and c.nurseId = :userId
              and c.isActive = 1
              and t.nodeKey = 'bed_reserve'
              and t.status in :statuses
            order by t.createdAt desc
            """)
    List<WfTask> findAdmissionBedReserveTodoForCareTeam(@Param("userId") Long userId,
                                                        @Param("statuses") Collection<String> statuses);

    @Query("select t from WfTask t where t.status in :statuses")
    Page<WfTask> findAllByStatusIn(@Param("statuses") Collection<String> statuses, Pageable pageable);

    List<WfTask> findByInstanceIdOrderByCreatedAtAsc(Long instanceId);

    Optional<WfTask> findFirstByInstanceIdOrderByCreatedAtDesc(Long instanceId);

    Optional<WfTask> findFirstByInstanceIdAndNodeKeyOrderByCreatedAtDesc(Long instanceId, String nodeKey);

    boolean existsByInstanceIdAndAssigneeId(Long instanceId, Long assigneeId);

    boolean existsByInstanceIdAndCandidateRole(Long instanceId, String candidateRole);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update WfTask t set t.status='claimed', t.assigneeId=:assigneeId, t.claimedAt=:claimedAt where t.wfTaskId=:taskId and t.status='pending'")
    int claimIfPending(@Param("taskId") Long taskId,
                       @Param("assigneeId") Long assigneeId,
                       @Param("claimedAt") LocalDateTime claimedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update WfTask t set t.status='completed', t.completedAt=:completedAt, t.comment=:comment, t.formDataJson=:formDataJson, t.attachmentsJson=:attachmentsJson where t.wfTaskId=:taskId and t.status in ('pending','claimed')")
    int completeIfPendingOrClaimed(@Param("taskId") Long taskId,
                                   @Param("completedAt") LocalDateTime completedAt,
                                   @Param("comment") String comment,
                                   @Param("formDataJson") String formDataJson,
                                   @Param("attachmentsJson") String attachmentsJson);
}
