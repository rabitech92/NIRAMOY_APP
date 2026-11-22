package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.PurchaseEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseEntryRepository extends JpaRepository<PurchaseEntry, Long> {

    /** মাসভিত্তিক মোট ক্রয়: [বছর, মাস, টাকা] */
    @Query("select year(p.entryDate), month(p.entryDate), sum(p.totalCost) from PurchaseEntry p "
            + "where (:branchId is null or p.branch.id = :branchId) group by year(p.entryDate), month(p.entryDate)")
    List<Object[]> monthlyPurchases(@Param("branchId") Long branchId);
}
