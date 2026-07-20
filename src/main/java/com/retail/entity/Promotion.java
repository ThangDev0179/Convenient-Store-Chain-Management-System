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

    @Column(name = "PromotionName", nullable = false, length = 200, columnDefinition = "NVARCHAR(200)")
    private String promotionName;

    @Column(name = "StartDateTime", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "EndDateTime", nullable = false)
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.Draft;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private Employee createdBy;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PromotionDetail> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void addDetail(PromotionDetail detail) {
        if (this.details == null) this.details = new ArrayList<>();
        detail.setPromotion(this);
        this.details.add(detail);
    }
}
