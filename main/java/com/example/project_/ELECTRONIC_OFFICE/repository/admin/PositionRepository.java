package com.example.project_.ELECTRONIC_OFFICE.repository.admin;

import com.example.project_.ELECTRONIC_OFFICE.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {

    // Lấy tất cả chức vụ theo companyId
    List<Position> findByCompanyId(UUID companyId);

    // Lấy chức vụ theo companyId và sắp xếp theo tên
    List<Position> findByCompanyIdOrderByPositionNameAsc(UUID companyId);


    // Kiểm tra tồn tại theo tên và companyId
    boolean existsByPositionNameAndCompanyId(String positionName, UUID companyId);

    // Tìm theo tên và companyId
    Optional<Position> findByPositionNameAndCompanyId(String positionName, UUID companyId);
}