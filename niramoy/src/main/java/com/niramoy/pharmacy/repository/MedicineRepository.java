package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    /** filter: ALL | LOW | OK | EXPIRING | EXPIRED */
    @Query("""
            select m from Medicine m
            where (:branchId is null or m.branch.id = :branchId)
              and (lower(m.name) like lower(concat('%', :q, '%'))
                   or m.barcode like concat('%', :q, '%')
                   or lower(m.batchNo) like lower(concat('%', :q, '%'))
                   or lower(cast(m.boxNo as string)) like lower(concat('%', :q, '%'))
                   or lower(m.genericName) like lower(concat('%', :q, '%'))
                   or lower(m.companyName) like lower(concat('%', :q, '%')))
              and (:filter = 'ALL'
                   or (:filter = 'LOW' and m.stock <= m.reorderLevel)
                   or (:filter = 'OK' and m.stock > m.reorderLevel)
                   or (:filter = 'EXPIRING' and m.expiryDate >= :today and m.expiryDate <= :soon)
                   or (:filter = 'EXPIRED' and m.expiryDate < :today))
            """)
    Page<Medicine> search(@Param("branchId") Long branchId, @Param("q") String q, @Param("filter") String filter,
                          @Param("today") LocalDate today, @Param("soon") LocalDate soon, Pageable pageable);

    /** POS: শুধু মেয়াদ-উত্তীর্ণ নয় এবং স্টক আছে এমন ব্যাচ, FEFO ক্রমে */
    @Query("""
            select m from Medicine m
            where m.branch.id = :branchId and m.stock > 0 and m.expiryDate >= :today
              and (lower(m.name) like lower(concat('%', :q, '%')) or m.barcode like concat('%', :q, '%')
                   or lower(cast(m.boxNo as string)) like lower(concat('%', :q, '%'))
                   or lower(m.genericName) like lower(concat('%', :q, '%'))
                   or lower(m.companyName) like lower(concat('%', :q, '%')))
            order by m.expiryDate asc, m.name asc
            """)
    List<Medicine> searchForSale(@Param("branchId") Long branchId, @Param("q") String q,
                                 @Param("today") LocalDate today, Pageable pageable);

    Optional<Medicine> findByBranchIdAndBarcodeAndBatchNo(Long branchId, String barcode, String batchNo);

    /** অ্যাটমিক স্টক কমানো — ওভার-সেল রোধ করে (0 ফেরত এলে স্টক যথেষ্ট ছিল না) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Medicine m set m.stock = m.stock - :qty where m.id = :id and m.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Medicine m set m.stock = m.stock + :qty where m.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Query("select count(m) from Medicine m where (:branchId is null or m.branch.id = :branchId)")
    long countAll(@Param("branchId") Long branchId);

    @Query("select count(m) from Medicine m where m.stock <= m.reorderLevel and (:branchId is null or m.branch.id = :branchId)")
    long countLowStock(@Param("branchId") Long branchId);

    @Query("""
            select count(m) from Medicine m
            where m.stock > 0 and m.expiryDate >= :from and m.expiryDate <= :to
              and (:branchId is null or m.branch.id = :branchId)
            """)
    long countExpiringBetween(@Param("branchId") Long branchId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select sum(m.costPrice * m.stock) from Medicine m
            where m.expiryDate < :today and (:branchId is null or m.branch.id = :branchId)
            """)
    BigDecimal expiredStockValue(@Param("branchId") Long branchId, @Param("today") LocalDate today);

    /** ওষুধের নাম অনুযায়ী মোট স্টক (সব ব্যাচ): [ছোট হাতের নাম, মোট পিস] */
    @Query("select lower(m.name), sum(m.stock) from Medicine m where (:branchId is null or m.branch.id = :branchId) group by lower(m.name)")
    List<Object[]> stockByName(@Param("branchId") Long branchId);

    // ---------- স্টকের মূল্য (ব্যবসায়িক সারাংশ) ----------
    /** [ক্রয়মূল্যে মোট, বিক্রয়মূল্যে মোট, মোট পিস, ব্যাচ সংখ্যা] */
    @Query("select sum(m.costPrice * m.stock), sum(m.salePrice * m.stock), sum(m.stock), count(m) from Medicine m "
            + "where m.stock > 0 and (:branchId is null or m.branch.id = :branchId)")
    List<Object[]> stockTotals(@Param("branchId") Long branchId);

    @Query("select sum(m.costPrice * m.stock) from Medicine m where m.stock > 0 and m.expiryDate >= :from "
            + "and m.expiryDate <= :to and (:branchId is null or m.branch.id = :branchId)")
    BigDecimal stockCostBetween(@Param("branchId") Long branchId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** শাখাভিত্তিক: [শাখা, ক্রয়মূল্য, বিক্রয়মূল্য, পিস] */
    @Query("select m.branch.name, sum(m.costPrice * m.stock), sum(m.salePrice * m.stock), sum(m.stock) from Medicine m "
            + "where m.stock > 0 and (:branchId is null or m.branch.id = :branchId) "
            + "group by m.branch.id, m.branch.name order by sum(m.costPrice * m.stock) desc")
    List<Object[]> stockByBranch(@Param("branchId") Long branchId);

    /** ক্যাটাগরিভিত্তিক: [ক্যাটাগরি, ক্রয়মূল্য, বিক্রয়মূল্য, পিস] */
    @Query("select coalesce(m.category, 'অন্যান্য'), sum(m.costPrice * m.stock), sum(m.salePrice * m.stock), sum(m.stock) "
            + "from Medicine m where m.stock > 0 and (:branchId is null or m.branch.id = :branchId) "
            + "group by coalesce(m.category, 'অন্যান্য') order by sum(m.costPrice * m.stock) desc")
    List<Object[]> stockByCategory(@Param("branchId") Long branchId);

    /** ওষুধভিত্তিক (সবচেয়ে বেশি টাকা আটকে আছে): [নাম, ক্রয়মূল্য, বিক্রয়মূল্য, পিস] */
    @Query("select min(m.name), sum(m.costPrice * m.stock), sum(m.salePrice * m.stock), sum(m.stock) from Medicine m "
            + "where m.stock > 0 and (:branchId is null or m.branch.id = :branchId) "
            + "group by lower(m.name) order by sum(m.costPrice * m.stock) desc")
    List<Object[]> topStockValue(@Param("branchId") Long branchId, org.springframework.data.domain.Pageable pageable);
}
