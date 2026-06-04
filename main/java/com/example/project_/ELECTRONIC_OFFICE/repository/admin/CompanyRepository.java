package com.example.project_.ELECTRONIC_OFFICE.repository.admin;

import com.example.project_.ELECTRONIC_OFFICE.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Company findByCompanyId(UUID companyId);

}
