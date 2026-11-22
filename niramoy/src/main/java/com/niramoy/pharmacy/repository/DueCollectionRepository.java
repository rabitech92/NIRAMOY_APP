package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.DueCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface DueCollectionRepository extends JpaRepository<DueCollection, Long> {
    @Query("""
            select sum(c.amount) from DueCollection c
            where c.createdAt >= :from and c.createdAt < :to
              and (:branchId is null or c.sale.branch.id = :branchId)
            """)
    BigDecimal sumBetween(@Param("branchId") Long branchId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
