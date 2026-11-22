package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.Expense;
import com.niramoy.pharmacy.entity.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    String FILTER = """
            (:branchId is null or e.branch.id = :branchId)
              and e.expenseDate >= :from and e.expenseDate <= :to
              and (:categoryId is null or e.category.id = :categoryId)
              and (:status is null or e.status = :status)
              and (:includeVoided = true or e.voided = false)
              and (lower(e.expenseNo) like lower(concat('%', :q, '%'))
                   or lower(coalesce(e.paidTo, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(e.referenceNo, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(e.description, '')) like lower(concat('%', :q, '%')))
            """;

    @Query("select e from Expense e where " + FILTER)
    Page<Expense> search(@Param("branchId") Long branchId, @Param("from") LocalDate from, @Param("to") LocalDate to,
                         @Param("categoryId") Long categoryId, @Param("status") ExpenseStatus status,
                         @Param("includeVoided") boolean includeVoided, @Param("q") String q, Pageable pageable);

    /** ফিল্টার করা তালিকার মোট: [খরচ, পরিশোধিত] — বাতিল খরচ বাদ */
    @Query("select sum(e.amount), sum(e.paidAmount) from Expense e where e.voided = false and "
            + "(:branchId is null or e.branch.id = :branchId) "
            + "and e.expenseDate >= :from and e.expenseDate <= :to "
            + "and (:categoryId is null or e.category.id = :categoryId) "
            + "and (:status is null or e.status = :status) "
            + "and (lower(e.expenseNo) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(e.paidTo, '')) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(e.referenceNo, '')) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(e.description, '')) like lower(concat('%', :q, '%')))")
    List<Object[]> totals(@Param("branchId") Long branchId, @Param("from") LocalDate from, @Param("to") LocalDate to,
                          @Param("categoryId") Long categoryId, @Param("status") ExpenseStatus status, @Param("q") String q);

    @Query("select sum(e.amount) from Expense e where e.voided = false and e.expenseDate >= :from and e.expenseDate <= :to "
            + "and (:branchId is null or e.branch.id = :branchId)")
    BigDecimal sumAmount(@Param("branchId") Long branchId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** মোট বকেয়া (এখনো পরিশোধ হয়নি) */
    @Query("select sum(e.amount - e.paidAmount) from Expense e where e.voided = false and e.paidAmount < e.amount "
            + "and (:branchId is null or e.branch.id = :branchId)")
    BigDecimal sumPending(@Param("branchId") Long branchId);

    /** ক্যাটাগরিভিত্তিক: [নাম, মোট] */
    @Query("select c.name, sum(e.amount) from Expense e join e.category c where e.voided = false "
            + "and e.expenseDate >= :from and e.expenseDate <= :to and (:branchId is null or e.branch.id = :branchId) "
            + "group by c.id, c.name order by sum(e.amount) desc")
    List<Object[]> sumByCategory(@Param("branchId") Long branchId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select e from Expense e where e.voided = false and (:branchId is null or e.branch.id = :branchId) "
            + "order by e.expenseDate desc, e.id desc")
    List<Expense> recent(@Param("branchId") Long branchId, Pageable pageable);
}
