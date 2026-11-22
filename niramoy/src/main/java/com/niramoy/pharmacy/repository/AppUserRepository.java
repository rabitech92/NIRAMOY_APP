package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.AppUser;
import com.niramoy.pharmacy.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    long countByActiveTrue();

    @Query("""
            select u from AppUser u
            where (:branchId is null or u.branch.id = :branchId)
              and (:role is null or u.role = :role)
              and (lower(u.fullName) like lower(concat('%', :q, '%')) or lower(u.username) like lower(concat('%', :q, '%')))
            """)
    Page<AppUser> search(@Param("branchId") Long branchId, @Param("role") Role role,
                         @Param("q") String q, Pageable pageable);
}
