package com.example.project_.ELECTRONIC_OFFICE.repository.user;

import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByUserName(String username);
}
