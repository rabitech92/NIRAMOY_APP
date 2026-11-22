package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.ExpenseSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseSubCategoryRepository extends JpaRepository<ExpenseSubCategory, Long> {
    List<ExpenseSubCategory> findAllByOrderByCategoryNameAscNameAsc();
    List<ExpenseSubCategory> findByActiveTrueOrderByNameAsc();
    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);
}
