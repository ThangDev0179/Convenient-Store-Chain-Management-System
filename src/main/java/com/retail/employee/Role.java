package com.retail.employee;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleId")
    private Integer roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "RoleCode", nullable = false, unique = true, length = 20)
    private RoleCode roleCode;

    @Column(name = "RoleName", nullable = false, length = 100)
    private String roleName;
}
