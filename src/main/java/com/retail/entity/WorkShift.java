package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "[WorkShift]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WorkShiftId")
    private Long workShiftId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "EmployeeId", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "BranchId", nullable = false)
    private Branch branch;

    @Column(name = "CheckInTime", nullable = false)
    private LocalDateTime checkInTime;

    @Column(name = "CheckOutTime")
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 50)
    private WorkShiftStatus status;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "workShift", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ShiftMetrics shiftMetrics;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
