package com.example.project_.ELECTRONIC_OFFICE.repository.admin;

import com.example.project_.ELECTRONIC_OFFICE.entity.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    Optional<Workflow> findByName(String name);

    List<Workflow> findByStatus(String status);

    Page<Workflow> findByStatus(String status, Pageable pageable);

    @Query("SELECT w FROM Workflow w WHERE w.status = :status ORDER BY w.updateAt DESC")
    List<Workflow> findAllActiveOrderByUpdated(@Param("status") String status);

    boolean existsByName(String name);
    List<Workflow> findByCompanyId(UUID companyId);

    // Lấy workflows active theo companyId
    List<Workflow> findByCompanyIdAndStatusOrderByCreateAtDesc(UUID companyId, String status);

    Optional<Workflow> findByCompanyIdAndNameAndStatus(UUID companyId, String name, String status);

    Optional<Workflow> findByCompanyIdAndName(UUID companyId, String name);

    // Tìm workflow active theo companyId và tên quy trình (requestType)
    default Optional<Workflow> findActiveByCompanyIdAndProcessCode(UUID companyId, String processCode) {
        return findByCompanyIdAndNameAndStatus(companyId, processCode, "ACTIVE");
    }


    // ✅ THÊM METHOD NÀY - Tìm tất cả workflow của công ty sắp xếp theo createAt giảm dần
    List<Workflow> findByCompanyIdOrderByCreateAtDesc(UUID companyId);


    // Tìm workflow đang active theo công ty
    List<Workflow> findByCompanyIdAndStatus(UUID companyId, String status);



    // Tìm workflow mới nhất theo companyId
    @Query("SELECT w FROM Workflow w WHERE w.companyId = :companyId ORDER BY w.createAt DESC")
    List<Workflow> findLatestByCompanyId(@Param("companyId") UUID companyId);

    // Kiểm tra workflow đã tồn tại chưa
    boolean existsByCompanyIdAndName(UUID companyId, String name);

    // Đếm số workflow theo companyId
    long countByCompanyId(UUID companyId);
}