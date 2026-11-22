package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.Sale;
import com.niramoy.pharmacy.entity.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
            select s from Sale s
            where (:branchId is null or s.branch.id = :branchId)
              and (:status is null or s.status = :status)
              and (lower(s.invoiceNo) like lower(concat('%', :q, '%')) or lower(coalesce(s.customerName, '')) like lower(concat('%', :q, '%')))
            """)
    Page<Sale> search(@Param("branchId") Long branchId, @Param("status") SaleStatus status,
                      @Param("q") String q, Pageable pageable);

    @Query("select s from Sale s left join fetch s.items where s.id = :id")
    Optional<Sale> findWithItemsById(@Param("id") Long id);

    // ---- enum literal-এর বদলে parameter ব্যবহার (Hibernate 6.5 FQN enum literal বোঝে না) ----

    @Query("""
            select distinct s from Sale s left join fetch s.items
            where s.status = :status
              and s.createdAt >= :from and s.createdAt < :to
              and (:branchId is null or s.branch.id = :branchId)
            order by s.createdAt desc
            """)
    List<Sale> queryByStatusBetween(@Param("status") SaleStatus status, @Param("branchId") Long branchId,
                                    @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select s from Sale s
            where s.status = :status and s.dueAmount > 0
              and (:branchId is null or s.branch.id = :branchId)
            order by s.createdAt asc
            """)
    List<Sale> queryOpenDues(@Param("status") SaleStatus status, @Param("branchId") Long branchId);

    @Query("""
            select sum(s.dueAmount) from Sale s
            where s.status = :status and s.dueAmount > 0
              and (:branchId is null or s.branch.id = :branchId)
            """)
    BigDecimal queryReceivable(@Param("status") SaleStatus status, @Param("branchId") Long branchId);

    /** রিপোর্ট ও ড্যাশবোর্ডের জন্য সম্পন্ন বিক্রয় (আইটেমসহ) */
    default List<Sale> findCompletedBetween(Long branchId, LocalDateTime from, LocalDateTime to) {
        return queryByStatusBetween(SaleStatus.COMPLETED, branchId, from, to);
    }

    default List<Sale> findOpenDues(Long branchId) {
        return queryOpenDues(SaleStatus.COMPLETED, branchId);
    }

    default BigDecimal totalReceivable(Long branchId) {
        return queryReceivable(SaleStatus.COMPLETED, branchId);
    }

    // ---------- গ্রাহক বিশ্লেষণ (গ্রাহক = মোবাইল নম্বর) ----------
    String CUSTOMER_WHERE = """
            where s.customerPhone is not null and s.status = :status
              and (:branchId is null or s.branch.id = :branchId)
              and (lower(coalesce(s.customerName, '')) like lower(concat('%', :q, '%')) or s.customerPhone like concat('%', :q, '%'))
            """;

    /** [মোবাইল, নাম, ইনভয়েস, মোট টাকা, শেষ ক্রয়, বকেয়া] — মোট টাকা অনুযায়ী সাজানো */
    @Query(value = "select s.customerPhone, max(s.customerName), count(s), sum(s.total), max(s.createdAt), sum(s.dueAmount) "
            + "from Sale s " + CUSTOMER_WHERE + " group by s.customerPhone order by sum(s.total) desc",
            countQuery = "select count(distinct s.customerPhone) from Sale s " + CUSTOMER_WHERE)
    Page<Object[]> customersByTotal(@Param("branchId") Long branchId, @Param("status") SaleStatus status,
                                    @Param("q") String q, Pageable pageable);

    /** একই, তবে শেষ ক্রয়ের তারিখ অনুযায়ী (সাম্প্রতিক আগে) */
    @Query(value = "select s.customerPhone, max(s.customerName), count(s), sum(s.total), max(s.createdAt), sum(s.dueAmount) "
            + "from Sale s " + CUSTOMER_WHERE + " group by s.customerPhone order by max(s.createdAt) desc",
            countQuery = "select count(distinct s.customerPhone) from Sale s " + CUSTOMER_WHERE)
    Page<Object[]> customersByRecent(@Param("branchId") Long branchId, @Param("status") SaleStatus status,
                                     @Param("q") String q, Pageable pageable);

    /** নির্দিষ্ট সময় থেকে এ পর্যন্ত গ্রাহকভিত্তিক মোট: [মোবাইল, মোট টাকা] */
    @Query("select s.customerPhone, sum(s.total) from Sale s where s.customerPhone in :phones and s.status = :status "
            + "and s.createdAt >= :from and (:branchId is null or s.branch.id = :branchId) group by s.customerPhone")
    List<Object[]> sumSince(@Param("phones") java.util.Collection<String> phones, @Param("status") SaleStatus status,
                            @Param("from") LocalDateTime from, @Param("branchId") Long branchId);

    /** সব গ্রাহকের সারাংশ: [গ্রাহক সংখ্যা, মোট টাকা] */
    @Query("select count(distinct s.customerPhone), sum(s.total) from Sale s where s.customerPhone is not null "
            + "and s.status = :status and (:branchId is null or s.branch.id = :branchId)")
    List<Object[]> customerTotals(@Param("branchId") Long branchId, @Param("status") SaleStatus status);

    @Query("select s from Sale s where s.customerPhone = :phone and s.status = :status "
            + "and (:branchId is null or s.branch.id = :branchId) order by s.createdAt desc")
    List<Sale> customerSales(@Param("phone") String phone, @Param("status") SaleStatus status,
                             @Param("branchId") Long branchId);

    @Query("select distinct s from Sale s left join fetch s.items where s.id in :ids order by s.createdAt desc")
    List<Sale> withItemsByIds(@Param("ids") java.util.Collection<Long> ids);

    // ---------- ব্যবসায়িক সারাংশ ----------
    /** মাসভিত্তিক: [বছর, মাস, ইনভয়েস, মোট বিক্রয়, মোট ছাড়] */
    @Query("select year(s.createdAt), month(s.createdAt), count(s), sum(s.total), sum(s.discount) from Sale s "
            + "where s.status = :status and (:branchId is null or s.branch.id = :branchId) "
            + "group by year(s.createdAt), month(s.createdAt)")
    List<Object[]> monthlySales(@Param("branchId") Long branchId, @Param("status") SaleStatus status);

    @Query("select sum(s.total) from Sale s where s.status = :status and s.createdAt >= :from and s.createdAt < :to "
            + "and (:branchId is null or s.branch.id = :branchId)")
    BigDecimal sumTotalBetween(@Param("branchId") Long branchId, @Param("status") SaleStatus status,
                               @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
