package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {
    @Query("select r from SaleReturn r where (:branchId is null or r.sale.branch.id = :branchId) order by r.id desc")
    List<SaleReturn> recent(@Param("branchId") Long branchId, org.springframework.data.domain.Pageable pageable);
}
