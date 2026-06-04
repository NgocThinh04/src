package com.example.project_.ELECTRONIC_OFFICE.repository.user;

import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByUserName(String username);

    Optional<Users> findByEmail(String email);


    List<Users> findByCompanyId(UUID companyId);

    List<Users> findByCompanyIdOrderByCreateAtDesc(UUID companyId);

    List<Users> findByStatus(String status);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);

    @Query("SELECT u FROM Users u WHERE u.companyId = :companyId AND u.status = 'ACTIVE'")
    List<Users> findActiveUsersByCompanyId(@Param("companyId") UUID companyId);
}
