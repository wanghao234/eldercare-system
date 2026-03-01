package com.wanghao.eldercare.eldercaresystem.careteam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CareTeamAssignmentRepository extends JpaRepository<CareTeamAssignment, Long>,
        JpaSpecificationExecutor<CareTeamAssignment> {

    @Query("select c.elderId from CareTeamAssignment c where c.nurseId = :nurseId and c.isActive = 1")
    List<Long> findActiveElderIdsByNurseId(@Param("nurseId") Long nurseId);

    @Query("select c.elderId from CareTeamAssignment c where c.familyId = :familyId and c.isActive = 1")
    List<Long> findActiveElderIdsByFamilyId(@Param("familyId") Long familyId);

    @Query("select count(c) > 0 from CareTeamAssignment c where c.elderId = :elderId and c.familyId = :familyId and c.isActive = 1")
    boolean existsActiveByElderIdAndFamilyId(@Param("elderId") Long elderId, @Param("familyId") Long familyId);

    @Query("select count(c) > 0 from CareTeamAssignment c where c.elderId = :elderId and c.nurseId = :nurseId and c.isActive = 1")
    boolean existsActiveByElderIdAndNurseId(@Param("elderId") Long elderId, @Param("nurseId") Long nurseId);

    @Query("select c.nurseId from CareTeamAssignment c where c.elderId = :elderId and c.isActive = 1 and c.nurseId is not null order by c.assignmentId asc")
    List<Long> findActiveNurseIdsByElderId(@Param("elderId") Long elderId);

    Optional<CareTeamAssignment> findByAssignmentId(Long assignmentId);

    @Modifying
    @Query("""
            UPDATE CareTeamAssignment c
            SET c.isActive = 0, c.updatedAt = :updatedAt
            WHERE c.assignmentId = :assignmentId AND c.isActive = 1
            """)
    int deactivateById(@Param("assignmentId") Long assignmentId, @Param("updatedAt") LocalDateTime updatedAt);
}
