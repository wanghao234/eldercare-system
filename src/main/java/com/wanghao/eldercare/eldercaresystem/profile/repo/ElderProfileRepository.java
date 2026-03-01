package com.wanghao.eldercare.eldercaresystem.profile.repo;

import com.wanghao.eldercare.eldercaresystem.profile.entity.ElderProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ElderProfileRepository extends JpaRepository<ElderProfileEntity, Long> {

    List<ElderProfileEntity> findByElderIdIn(Collection<Long> elderIds);

    @Query("""
            SELECT e.elderId FROM ElderProfileEntity e
            WHERE LOWER(e.careLevel) = LOWER(:careLevel)
            """)
    List<Long> findElderIdsByCareLevel(@Param("careLevel") String careLevel);

    @Query("""
            SELECT e.elderId FROM ElderProfileEntity e
            WHERE LOWER(e.careLevel) = LOWER(:careLevel)
              AND e.elderId IN :elderIds
            """)
    List<Long> findElderIdsByCareLevelAndElderIds(@Param("careLevel") String careLevel, @Param("elderIds") Collection<Long> elderIds);
}
