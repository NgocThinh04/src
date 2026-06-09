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

    // ==================== FIND BY BASIC FIELDS ====================

    Optional<Users> findByUserName(String username);

    Optional<Users> findByEmail(String email);

    Optional<Users> findByUserId(UUID userId);


    // ==================== FIND BY COMPANY ====================

    List<Users> findByCompanyId(UUID companyId);

    List<Users> findByCompanyIdOrderByCreateAtDesc(UUID companyId);

    // ✅ THÊM METHOD NÀY - Quan trọng cho luồng duyệt
    List<Users> findByCompanyIdAndPosition(UUID companyId, String position);

    // Tìm user theo company và position và status ACTIVE
    List<Users> findByCompanyIdAndPositionAndStatus(UUID companyId, String position, String status);


    // ==================== FIND BY STATUS ====================

    List<Users> findByStatus(String status);


    // ==================== FIND ACTIVE USERS ====================

    @Query("SELECT u FROM Users u WHERE u.companyId = :companyId AND u.status = 'ACTIVE'")
    List<Users> findActiveUsersByCompanyId(@Param("companyId") UUID companyId);

    // Tìm user active theo company và position
    @Query("SELECT u FROM Users u WHERE u.companyId = :companyId AND u.position = :position AND u.status = 'ACTIVE'")
    List<Users> findActiveUsersByCompanyIdAndPosition(@Param("companyId") UUID companyId,
                                                      @Param("position") String position);


    // ==================== EXISTS CHECKS ====================

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);


    // ==================== FIND BY POSITION (không cần companyId) ====================

    List<Users> findByPosition(String position);


    // ==================== TÌM NGƯỜI DUYỆT THEO VAI TRÒ (cho luồng duyệt) ====================

    /**
     * Tìm tất cả user có vai trò (position) trong danh sách
     * Dùng cho trường hợp cần lấy nhiều role cùng lúc
     */
    @Query("SELECT u FROM Users u WHERE u.companyId = :companyId AND u.position IN :positions AND u.status = 'ACTIVE'")
    List<Users> findByCompanyIdAndPositions(@Param("companyId") UUID companyId,
                                            @Param("positions") List<String> positions);

    /**
     * Đếm số lượng user theo vai trò trong công ty
     */
    long countByCompanyIdAndPosition(UUID companyId, String position);
}
