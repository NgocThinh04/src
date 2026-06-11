package com.example.project_.ELECTRONIC_OFFICE.repository.admin;

import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Users, UUID> {
    boolean existsByEmail(String email);
    @Query("SELECT COUNT(u) FROM Users u WHERE u.companyId = :companyId")
    Long countByCompanyId(@Param("companyId") UUID companyId);

    @Query("SELECT u FROM Users u WHERE u.companyId = :companyId ORDER BY u.createAt DESC")
    List<Users> findTop5ByCompanyIdOrderByCreateAtDesc(@Param("companyId") UUID companyId, Pageable pageable);
}
