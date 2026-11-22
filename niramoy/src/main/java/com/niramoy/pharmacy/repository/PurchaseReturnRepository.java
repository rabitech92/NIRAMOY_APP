package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.PurchaseReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {
    @Query("""
            select r from PurchaseReturn r
            where (:branchId is null or r.order.branch.id = :branchId)
              and lower(r.order.orderNo) like lower(concat('%', :q, '%'))
            """)
    Page<PurchaseReturn> search(@Param("branchId") Long branchId, @Param("q") String q, Pageable pageable);

    /** মাসভিত্তিক ক্রয় ফেরত: [বছর, মাস, টাকা] */
    @Query("select year(r.createdAt), month(r.createdAt), sum(r.amount) from PurchaseReturn r "
            + "where (:branchId is null or r.order.branch.id = :branchId) group by year(r.createdAt), month(r.createdAt)")
    List<Object[]> monthlyReturns(@Param("branchId") Long branchId);
}
