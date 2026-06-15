package com.example.project_.ELECTRONIC_OFFICE.repository.admin;

import com.example.project_.ELECTRONIC_OFFICE.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {


    @Query("SELECT w FROM Workflow w WHERE w.status = :status ORDER BY w.updateAt DESC")
    List<Workflow> findAllActiveOrderByUpdated(@Param("status") String status);

    boolean existsByName(String name);
    List<Workflow> findByCompanyId(UUID companyId);

    // Lấy workflows active theo companyId
    List<Workflow> findByCompanyIdAndStatusOrderByCreateAtDesc(UUID companyId, String status);

    Optional<Workflow> findByCompanyIdAndNameAndStatus(UUID companyId, String name, String status);

}