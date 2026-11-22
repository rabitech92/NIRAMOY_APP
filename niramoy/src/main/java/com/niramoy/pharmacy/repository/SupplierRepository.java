package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findAllByOrderByNameAsc();

    @Query("""
            select s from Supplier s
            where (lower(s.name) like lower(concat('%', :q, '%')) or s.phone like concat('%', :q, '%'))
              and (:filter = 'ALL' or (:filter = 'DUE' and s.dueAmount > 0) or (:filter = 'PAID' and s.dueAmount = 0))
            """)
    Page<Supplier> search(@Param("q") String q, @Param("filter") String filter, Pageable pageable);

    @Query("select sum(s.dueAmount) from Supplier s")
    BigDecimal totalDue();
}
