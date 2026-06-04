package com.example.project_.ELECTRONIC_OFFICE.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "positions")
public class Position {
    @Id
    @Column(name = "position_id", columnDefinition = "UUID", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID positionId;

    @Column(name = "position_name")
    private String positionName;

    @Column(name = "company_id")
    private UUID companyId;
    @PrePersist
    public void prePersist() {
        if (positionId == null) {
            positionId = UUID.randomUUID();
        }
    }
}
