package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
    List<StockAdjustment> findTop10ByMedicineIdOrderByIdDesc(Long medicineId);
}
