package com.wanghao.eldercare.eldercaresystem.inventory;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SupplyStockRepository extends JpaRepository<SupplyStock, Long> {

    @Query("""
            select s from SupplyStock s
            where (:itemId is null or s.supplyItemId = :itemId)
              and (:location is null or lower(s.location) like lower(concat('%', :location, '%')))
            """)
    Page<SupplyStock> search(@Param("itemId") Long itemId,
                             @Param("location") String location,
                             Pageable pageable);

    boolean existsBySupplyItemId(Long supplyItemId);

    Optional<SupplyStock> findTopBySupplyItemIdAndLocationOrderByStockIdDesc(Long supplyItemId, String location);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            update supply_stocks
            set quantity = quantity - :delta, updated_at = :updatedAt
            where stock_id = :stockId and quantity >= :delta
            """, nativeQuery = true)
    int deductIfEnough(@Param("stockId") Long stockId,
                       @Param("delta") BigDecimal delta,
                       @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            update supply_stocks
            set quantity = quantity + :delta, updated_at = :updatedAt
            where stock_id = :stockId
            """, nativeQuery = true)
    int addQuantity(@Param("stockId") Long stockId,
                    @Param("delta") BigDecimal delta,
                    @Param("updatedAt") LocalDateTime updatedAt);
}
