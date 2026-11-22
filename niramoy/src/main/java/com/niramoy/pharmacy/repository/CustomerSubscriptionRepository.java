package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.CustomerSubscription;
import com.niramoy.pharmacy.entity.RefillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface CustomerSubscriptionRepository extends JpaRepository<CustomerSubscription, Long> {

    @Query("""
            select s from CustomerSubscription s
            where (:branchId is null or s.branch.id = :branchId)
              and (:status is null or s.status = :status)
              and s.nextRefillDate <= :dueUntil
              and (lower(s.customerName) like lower(concat('%', :q, '%'))
                   or s.customerPhone like concat('%', :q, '%')
                   or lower(s.planName) like lower(concat('%', :q, '%')))
            """)
    Page<CustomerSubscription> search(@Param("branchId") Long branchId, @Param("status") RefillStatus status,
                                      @Param("q") String q, @Param("dueUntil") LocalDate dueUntil, Pageable pageable);

    @Query("""
            select count(s) from CustomerSubscription s
            where s.status = :status and s.nextRefillDate <= :date
              and (:branchId is null or s.branch.id = :branchId)
            """)
    long countDue(@Param("branchId") Long branchId, @Param("status") RefillStatus status, @Param("date") LocalDate date);
}
