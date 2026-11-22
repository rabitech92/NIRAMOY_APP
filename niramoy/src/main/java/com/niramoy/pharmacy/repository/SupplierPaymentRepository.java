package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
    List<SupplierPayment> findTop10ByOrderByIdDesc();
}
