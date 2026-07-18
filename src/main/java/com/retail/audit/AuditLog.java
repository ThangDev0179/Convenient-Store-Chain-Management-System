package com.retail.audit;

import com.retail.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "AuditLog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AuditLogId")
    private Long auditLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EmployeeId")
    private Employee employee;

    @Column(name = "ActionType", nullable = false, length = 50)
    private String actionType;

    @Column(name = "EntityName", nullable = false, length = 50)
    private String entityName;

    @Column(name = "EntityId", nullable = false)
    private Long entityId;

    @Lob
    @Column(name = "OldValue")
    private String oldValue;

    @Lob
    @Column(name = "NewValue")
    private String newValue;

    @Column(name = "Reason", length = 500)
    private String reason;

    @Column(name = "IpAddress", length = 50)
    private String ipAddress;

    @Column(name = "DeviceId", length = 200)
    private String deviceId;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
