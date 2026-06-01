package com.example.project_.ELECTRONIC_OFFICE.repository.admin;

import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminRepository extends JpaRepository<Users, UUID> {
    boolean existsByEmail(String email);
}
