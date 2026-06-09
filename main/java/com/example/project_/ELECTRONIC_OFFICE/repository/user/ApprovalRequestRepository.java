package com.example.project_.ELECTRONIC_OFFICE.repository.user;

import com.example.project_.ELECTRONIC_OFFICE.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {


    // Lấy theo company và trạng thái
    List<ApprovalRequest> findByCompanyIdAndStatusOrderByCreatedAtDesc(UUID companyId, String status);

    // Lấy theo company (tất cả)
    List<ApprovalRequest> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    // Lấy theo người gửi
    List<ApprovalRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    // Lấy theo mã yêu cầu
    List<ApprovalRequest> findByRequestCode(String requestCode);

    // Lấy theo company và requestType
    List<ApprovalRequest> findByCompanyIdAndRequestTypeOrderByCreatedAtDesc(UUID companyId, String requestType);

    // Đếm số lượng theo trạng thái
    long countByCompanyIdAndStatus(UUID companyId, String status);

    // ✅ THÊM METHOD NÀY - Tìm request đang chờ duyệt theo vai trò của người duyệt
    @Query("SELECT DISTINCT ar FROM ApprovalRequest ar " +
            "JOIN ApprovalAction aa ON aa.requestId = ar.id " +
            "WHERE ar.companyId = :companyId " +
            "AND ar.status = 'PENDING' " +
            "AND aa.action = 'PENDING' " +
            "AND aa.stepName = :position " +
            "ORDER BY ar.createdAt DESC")
    List<ApprovalRequest> findPendingRequestsByApproverRole(
            @Param("companyId") UUID companyId,
            @Param("position") String position);
    // Method mới - lấy TẤT CẢ (không phân biệt trạng thái)
    @Query("SELECT DISTINCT ar FROM ApprovalRequest ar " +
            "JOIN ApprovalAction aa ON aa.requestId = ar.id " +
            "WHERE ar.companyId = :companyId " +
            "AND aa.stepName = :position " +
            "AND ar.requesterId != :userId " +  // Không lấy yêu cầu do chính user gửi
            "ORDER BY ar.createdAt DESC")
    List<ApprovalRequest> findRequestsByApproverRole(
            @Param("companyId") UUID companyId,
            @Param("position") String position,
            @Param("userId") UUID userId);
    // Tìm request đang chờ duyệt cho nhiều vai trò
    @Query("SELECT DISTINCT ar FROM ApprovalRequest ar " +
            "JOIN ApprovalAction aa ON aa.requestId = ar.id " +
            "WHERE ar.companyId = :companyId " +
            "AND ar.status = 'PENDING' " +
            "AND aa.action = 'PENDING' " +
            "AND aa.stepName IN :positions " +
            "ORDER BY ar.createdAt DESC")
    List<ApprovalRequest> findPendingRequestsByApproverRoles(
            @Param("companyId") UUID companyId,
            @Param("positions") List<String> positions);

    // Tìm request theo người duyệt (đã duyệt)
    @Query("SELECT DISTINCT ar FROM ApprovalRequest ar " +
            "JOIN ApprovalAction aa ON aa.requestId = ar.id " +
            "WHERE aa.approverId = :approverId " +
            "ORDER BY ar.createdAt DESC")
    List<ApprovalRequest> findByApproverId(@Param("approverId") UUID approverId);
}