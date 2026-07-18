package com.retail.auth;

import com.retail.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ActiveSession")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveSession {

    @Id
    @Column(name = "EmployeeId")
    private Long employeeId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "EmployeeId")
    private Employee employee;

    @Column(name = "SessionToken", nullable = false, length = 500)
    private String sessionToken;

    @Column(name = "DeviceId", length = 200)
    private String deviceId;

    @Column(name = "IpAddress", length = 50)
    private String ipAddress;

    @Column(name = "LoginAt", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "ExpiresAt")
    private LocalDateTime expiresAt;
}
