package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "[Branch]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BranchId")
    private Long branchId;

    @Column(name = "BranchName", nullable = false, length = 100)
    private String branchName;
}
