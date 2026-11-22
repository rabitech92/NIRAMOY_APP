package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByActiveTrueOrderByNameAsc();
    List<Branch> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    long countByActiveTrue();
}
