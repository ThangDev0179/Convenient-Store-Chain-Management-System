package com.retail.common.stub;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * STUB — sẽ được thay bằng entity chính thức của thành viên 2 khi merge.
 */
@Entity
@Table(name = "Promotion")
@Getter
@Setter
@NoArgsConstructor
public class PromotionStub {

    @Id
    @Column(name = "PromotionId")
    private Long promotionId;

    @Column(name = "PromotionName", length = 255)
    private String promotionName;

    @Column(name = "StartDateTime")
    private LocalDateTime startDateTime;

    @Column(name = "EndDateTime")
    private LocalDateTime endDateTime;

    @Column(name = "Status", length = 20)
    private String status; // Active | Inactive | Expired
}
