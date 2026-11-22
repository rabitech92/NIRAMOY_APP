package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.OrderStatus;
import com.niramoy.pharmacy.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    @Query("""
            select o from PurchaseOrder o
            where (:branchId is null or o.branch.id = :branchId)
              and (:status is null or o.status = :status)
              and (lower(o.orderNo) like lower(concat('%', :q, '%')) or lower(o.supplier.name) like lower(concat('%', :q, '%')))
            """)
    Page<PurchaseOrder> search(@Param("branchId") Long branchId, @Param("status") OrderStatus status,
                               @Param("q") String q, Pageable pageable);
}
