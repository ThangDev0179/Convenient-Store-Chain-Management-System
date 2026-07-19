package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Promotion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromotionId")
    private Long promotionId;

    @Column(name = "PromotionName", nullable = false, length = 200)
    private String promotionName;

    @Column(name = "StartDateTime", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "EndDateTime", nullable = false)
    private LocalDateTime endDateTime;

    @Column(name = "Status", nullable = false, length = 20)
    private String status; // Draft | Active | Ended | Canceled

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private Employee createdBy;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PromotionDetail> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "Draft";
    }

    public void addDetail(PromotionDetail detail) {
        details.add(detail);
        detail.setPromotion(this);
    }
}
