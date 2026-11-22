package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.AppSubscription;
import com.niramoy.pharmacy.entity.SubscriptionState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AppSubscriptionRepository extends JpaRepository<AppSubscription, Long> {

    /** আজকের তারিখে চালু সাবস্ক্রিপশন (সবচেয়ে পরে শেষ হওয়াটি আগে) */
    @Query("""
            select s from AppSubscription s
            where s.state = :state and s.startDate <= :d and s.endDate >= :d
            order by s.endDate desc
            """)
    List<AppSubscription> findCurrent(@Param("state") SubscriptionState state, @Param("d") LocalDate d, Pageable p);

    @Query("select max(s.endDate) from AppSubscription s where s.state = :state")
    LocalDate latestEnd(@Param("state") SubscriptionState state);

    boolean existsByState(SubscriptionState state);

    List<AppSubscription> findTop50ByOrderByIdDesc();

    List<AppSubscription> findByStateOrderByIdAsc(SubscriptionState state);
}
