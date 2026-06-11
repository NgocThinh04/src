package com.example.project_.ELECTRONIC_OFFICE.repository.user;

import com.example.project_.ELECTRONIC_OFFICE.entity.ApprovalAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, UUID> {

    // Lấy tất cả actions của 1 request (theo thứ tự bước)
    List<ApprovalAction> findByRequestIdOrderByStepOrderAsc(UUID requestId);

    // Lấy action theo requestId và stepOrder
    List<ApprovalAction> findByRequestIdAndStepOrder(UUID requestId, Integer stepOrder);

    // ✅ THÊM METHOD NÀY - Lấy action theo requestId và action (PENDING, APPROVED, REJECTED)
    List<ApprovalAction> findByRequestIdAndAction(UUID requestId, String action);

    // Lấy action theo requestId, stepOrder và action
    List<ApprovalAction> findByRequestIdAndStepOrderAndAction(UUID requestId, Integer stepOrder, String action);

    // ✅ THÊM METHOD NÀY - Lấy action theo requestId và stepOrder > giá trị và action = PENDING
    @Query("SELECT aa FROM ApprovalAction aa WHERE aa.requestId = :requestId AND aa.stepOrder > :stepOrder AND aa.action = :action ORDER BY aa.stepOrder ASC")
    List<ApprovalAction> findByRequestIdAndStepOrderGreaterThanAndAction(
            @Param("requestId") UUID requestId,
            @Param("stepOrder") Integer stepOrder,
            @Param("action") String action);

    // Lấy action pending đầu tiên của request
    @Query("SELECT aa FROM ApprovalAction aa WHERE aa.requestId = :requestId AND aa.action = 'PENDING' ORDER BY aa.stepOrder ASC")
    List<ApprovalAction> findFirstPendingActionByRequestId(@Param("requestId") UUID requestId);

    // Đếm số action đã APPROVED trong 1 bước
    long countByRequestIdAndStepOrderAndAction(UUID requestId, Integer stepOrder, String action);

    // Kiểm tra xem user đã duyệt ở bước nào chưa
    boolean existsByRequestIdAndApproverIdAndStepOrder(UUID requestId, UUID approverId, Integer stepOrder);

    @Query("SELECT COALESCE(MAX(a.stepOrder), 0) FROM ApprovalAction a WHERE a.requestId = :requestId")
    Integer findMaxStepOrderByRequestId(@Param("requestId") UUID requestId);
    @Query("SELECT a FROM ApprovalAction a WHERE a.requestId IN :requestIds AND a.actionStatus = 'COMPLETED' ORDER BY a.approvedAt DESC")
    List<ApprovalAction> findTop10ByRequestIdInOrderByApprovedAtDesc(@Param("requestIds") List<UUID> requestIds, Pageable pageable);
}
