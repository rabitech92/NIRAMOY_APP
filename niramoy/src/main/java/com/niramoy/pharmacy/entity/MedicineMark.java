package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** "বেশি বিক্রিত" চিহ্নিত ওষুধ (ওষুধের নাম অনুযায়ী, সব ব্যাচ ও শাখার জন্য প্রযোজ্য) */
@Entity
@Table(name = "medicine_marks", uniqueConstraints = @UniqueConstraint(name = "uk_mark_key", columnNames = "nameKey"))
@Getter @Setter @NoArgsConstructor
public class MedicineMark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** নাম ছোট হাতের + ছাঁটা (মিলানোর জন্য) */
    @Column(nullable = false, length = 150)
    private String nameKey;

    @Column(nullable = false, length = 150)
    private String displayName;

    /** "শীর্ষ N চিহ্নিত করুন" বোতামে স্বয়ংক্রিয়ভাবে হয়েছে কিনা */
    @Column(nullable = false)
    private boolean autoMarked;

    /** চিহ্নিত করার সময়ের পরিসংখ্যান (কেন চিহ্নিত হয়েছিল তা মনে রাখতে) */
    @Column(nullable = false)
    private long soldPieces;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(length = 60)
    private String periodLabel;

    @Column(length = 100)
    private String markedBy;

    @Column(nullable = false)
    private LocalDateTime markedAt;

    @PrePersist void onCreate() { if (markedAt == null) markedAt = LocalDateTime.now(); }
}
