package com.example.project_.ELECTRONIC_OFFICE.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
public class Users {
    @Id
    @Column(name = "user_id", columnDefinition = "UUID", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(name = "name")
    private String name;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "password")
    private String passWord;

    @Column(name = "role")
    private String role;

    @Column(name = "address")
    private String address;

    @Column(name = "number")
    private String number;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "email")
    private String email;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "status")
    private String status;

    @Column(name = "position")
    private String position;

    @Column(name = "create_at")
    private OffsetDateTime createAt;

    @Column(name = "update_at")
    private OffsetDateTime updateAt;

    @PrePersist
    public void prePersist() {
        if (userId == null) {
            userId = UUID.randomUUID();
        }
        if (createAt == null) {
            createAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }
    @PreUpdate
    public void preUpdate() {
        updateAt = OffsetDateTime.now();
    }

}
