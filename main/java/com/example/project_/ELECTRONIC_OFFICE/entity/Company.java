package com.example.project_.ELECTRONIC_OFFICE.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "company")
public class Company {
    @Id
    @Column(name = "company_id", columnDefinition = "UUID", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID companyId;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "create_at")
    private OffsetDateTime createAt;

    @PrePersist
    public void prePersist() {
        if (companyId == null) {
            companyId = UUID.randomUUID();
        }
        if (createAt == null) {
            createAt = OffsetDateTime.now();
        }
    }
}
