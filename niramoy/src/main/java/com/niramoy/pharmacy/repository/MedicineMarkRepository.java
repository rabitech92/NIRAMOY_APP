package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.MedicineMark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicineMarkRepository extends JpaRepository<MedicineMark, Long> {
    Optional<MedicineMark> findByNameKey(String nameKey);
    List<MedicineMark> findAllByOrderByMarkedAtDesc();
    long deleteByNameKey(String nameKey);
}
