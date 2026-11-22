package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.ExpensePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpensePaymentRepository extends JpaRepository<ExpensePayment, Long> {

    /** পেমেন্ট পদ্ধতিভিত্তিক ব্যয়: [PaymentMethod, মোট] */
    @Query("""
            select p.method, sum(p.amount) from ExpensePayment p
            where p.expense.voided = false and p.paidOn >= :from and p.paidOn <= :to
              and (:branchId is null or p.expense.branch.id = :branchId)
            group by p.method
            order by sum(p.amount) desc
            """)
    List<Object[]> sumByMethod(@Param("branchId") Long branchId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
