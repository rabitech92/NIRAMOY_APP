package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.SaleItem;
import com.niramoy.pharmacy.entity.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    /** ওষুধের নাম অনুযায়ী বিক্রয়: [নাম, মোট পিস, মোট টাকা, ইনভয়েস সংখ্যা] (ফেরত দেওয়া ইনভয়েস বাদ) */
    @Query("""
            select min(i.medicineName), sum(i.quantity * i.packSize), sum(i.lineTotal), count(distinct i.sale.id)
            from SaleItem i
            where i.sale.status = :status and i.sale.createdAt >= :from and i.sale.createdAt < :to
              and (:branchId is null or i.sale.branch.id = :branchId)
            group by lower(i.medicineName)
            """)
    List<Object[]> topSelling(@Param("branchId") Long branchId, @Param("status") SaleStatus status,
                              @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** একজন গ্রাহকের ওষুধভিত্তিক কেনা: [নাম, মোট পিস, মোট টাকা, কতবার (ইনভয়েস)] */
    @Query("""
            select min(i.medicineName), sum(i.quantity * i.packSize), sum(i.lineTotal), count(distinct i.sale.id)
            from SaleItem i
            where i.sale.customerPhone = :phone and i.sale.status = :status
              and (:branchId is null or i.sale.branch.id = :branchId)
            group by lower(i.medicineName)
            """)
    List<Object[]> customerTop(@Param("phone") String phone, @Param("status") SaleStatus status,
                               @Param("branchId") Long branchId);

    /** মাসভিত্তিক মোট মার্জিন (বিক্রয় − ক্রয়মূল্য): [বছর, মাস, মার্জিন] — ছাড় আলাদাভাবে বাদ দিতে হয় */
    @Query("select year(i.sale.createdAt), month(i.sale.createdAt), sum((i.unitPrice - i.costPrice) * i.quantity) "
            + "from SaleItem i where i.sale.status = :status and (:branchId is null or i.sale.branch.id = :branchId) "
            + "group by year(i.sale.createdAt), month(i.sale.createdAt)")
    List<Object[]> monthlyMargin(@Param("branchId") Long branchId, @Param("status") SaleStatus status);
}
