package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.StockSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockSnapshotRepository extends JpaRepository<StockSnapshot, Long> {
    Optional<StockSnapshot> findByBranchIdAndSnapshotDate(Long branchId, LocalDate date);

    /** দিনভিত্তিক মোট (শাখা অনুযায়ী যোগফল), তারিখ ক্রমে: [তারিখ, ক্রয়মূল্য, বিক্রয়মূল্য] */
    @Query("select s.snapshotDate, sum(s.costValue), sum(s.saleValue) from StockSnapshot s "
            + "where (:branchId is null or s.branch.id = :branchId) group by s.snapshotDate order by s.snapshotDate")
    List<Object[]> byDate(@Param("branchId") Long branchId);
}
