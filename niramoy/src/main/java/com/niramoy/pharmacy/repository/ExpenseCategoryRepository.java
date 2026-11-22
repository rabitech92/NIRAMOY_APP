package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    List<ExpenseCategory> findAllByOrderByGroupTypeAscNameAsc();
    List<ExpenseCategory> findByActiveTrueOrderByGroupTypeAscNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
