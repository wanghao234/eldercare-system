package com.wanghao.eldercare.eldercaresystem.admission;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BedRepository extends JpaRepository<Bed, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Bed b set b.status='occupied' where b.bedId=:bedId and b.status='available'")
    int occupyIfAvailable(@Param("bedId") Long bedId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Bed b set b.status='available' where b.bedId=:bedId")
    int releaseAsAvailable(@Param("bedId") Long bedId);
}
