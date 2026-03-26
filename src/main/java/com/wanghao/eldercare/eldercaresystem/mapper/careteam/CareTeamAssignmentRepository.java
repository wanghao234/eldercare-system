package com.wanghao.eldercare.eldercaresystem.mapper.careteam;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careteam.*;
import com.wanghao.eldercare.eldercaresystem.dto.careteam.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.*;
import com.wanghao.eldercare.eldercaresystem.service.careteam.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CareTeamAssignmentRepository extends JpaRepository<CareTeamAssignment, Long>,
        JpaSpecificationExecutor<CareTeamAssignment> {

    @Query("select c.elderId from CareTeamAssignment c where c.nurseId = :nurseId and c.isActive = 1")
    List<Long> findActiveElderIdsByNurseId(@Param("nurseId") Long nurseId);

    @Query("select c.elderId from CareTeamAssignment c where c.familyId = :familyId and c.isActive = 1")
    List<Long> findActiveElderIdsByFamilyId(@Param("familyId") Long familyId);

    @Query("select count(c) > 0 from CareTeamAssignment c where c.elderId = :elderId and c.familyId = :familyId and c.isActive = 1")
    boolean existsActiveByElderIdAndFamilyId(@Param("elderId") Long elderId, @Param("familyId") Long familyId);

    @Query("select count(c) > 0 from CareTeamAssignment c where c.elderId = :elderId and c.familyId is not null and c.isActive = 1")
    boolean existsActiveFamilyByElderId(@Param("elderId") Long elderId);

    @Query("""
            select count(c) > 0
            from CareTeamAssignment c
            where c.elderId = :elderId
              and c.familyId is not null
              and c.isActive = 1
              and c.assignmentId <> :assignmentId
            """)
    boolean existsActiveFamilyByElderIdAndAssignmentIdNot(@Param("elderId") Long elderId,
                                                          @Param("assignmentId") Long assignmentId);

    @Query("select count(c) > 0 from CareTeamAssignment c where c.elderId = :elderId and c.nurseId = :nurseId and c.isActive = 1")
    boolean existsActiveByElderIdAndNurseId(@Param("elderId") Long elderId, @Param("nurseId") Long nurseId);

    @Query("select c.nurseId from CareTeamAssignment c where c.elderId = :elderId and c.isActive = 1 and c.nurseId is not null order by c.assignmentId asc")
    List<Long> findActiveNurseIdsByElderId(@Param("elderId") Long elderId);

    Optional<CareTeamAssignment> findFirstByElderIdAndIsActiveOrderByAssignmentIdAsc(Long elderId, Integer isActive);

    List<CareTeamAssignment> findAllByElderIdAndIsActiveAndNurseIdIsNotNullOrderByAssignmentIdAsc(Long elderId, Integer isActive);

    List<CareTeamAssignment> findAllByElderIdAndIsActiveAndFamilyIdIsNotNullOrderByAssignmentIdAsc(Long elderId, Integer isActive);

    Optional<CareTeamAssignment> findByElderIdAndNurseIdAndIsActive(Long elderId, Long nurseId, Integer isActive);

    Optional<CareTeamAssignment> findByAssignmentId(Long assignmentId);

    @Modifying
    @Query("""
            UPDATE CareTeamAssignment c
            SET c.isActive = 0, c.updatedAt = :updatedAt
            WHERE c.assignmentId = :assignmentId AND c.isActive = 1
            """)
    int deactivateById(@Param("assignmentId") Long assignmentId, @Param("updatedAt") LocalDateTime updatedAt);
}
