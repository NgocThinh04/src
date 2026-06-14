package com.example.project_.ELECTRONIC_OFFICE.service.admin;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.RegisterRequestAdmin;
import com.example.project_.ELECTRONIC_OFFICE.entity.Company;
import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import com.example.project_.ELECTRONIC_OFFICE.exception.BadRequestException;
import com.example.project_.ELECTRONIC_OFFICE.mapper.AdminMapper;
import com.example.project_.ELECTRONIC_OFFICE.repository.admin.AdminRepository;
import com.example.project_.ELECTRONIC_OFFICE.repository.admin.CompanyRepository;
import com.example.project_.ELECTRONIC_OFFICE.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;
    private final CompanyRepository companyRepository;
//    @Transactional(rollbackFor = Exception.class)
//    public void registerAdmin(RegisterRequestAdmin registerRequestAdmin) throws BadRequestException {
//        // Kiểm tra email đã tồn tại chưa
//        boolean exists = adminRepository.existsByEmail(registerRequestAdmin.getEmail());
//        if (exists) {
//            log.warn("Register failed - email exists: {}", registerRequestAdmin.getEmail());
//            throw new BadRequestException("Admin with email " + registerRequestAdmin.getEmail() + " already exists");
//        }
//        Company company = new Company();
//        company.setName(registerRequestAdmin.getNameCompany());
//        company.setAddress(registerRequestAdmin.getAddress());
//        String companyCode = generateCompanyCode();
//        company.setCompanyCode(companyCode);
//
//        Company saved = companyRepository.save(company);
//        // Chuyển đổi từ RegisterRequestAdmin sang Users
//        Users user = AdminMapper.toUserEntity(registerRequestAdmin);
//        user.setRole("Admin");
//        user.setCompanyId(saved.getCompanyId());
//        user.setCompanyCode(saved.getCompanyCode());
//        String encodedPassword = passwordEncoder.encode(registerRequestAdmin.getPassword());
//        user.setPassWord(encodedPassword);  // Lưu password đã encode
//        // Lưu vào database
//        adminRepository.save(user);
//
//        log.info("Admin registered successfully: {}", registerRequestAdmin.getEmail());
//    }
@Autowired
private MailService mailService;

    @Transactional(rollbackFor = Exception.class)
    public void registerAdmin(RegisterRequestAdmin registerRequestAdmin) throws BadRequestException {
        // Kiểm tra email đã tồn tại chưa
        boolean exists = adminRepository.existsByEmail(registerRequestAdmin.getEmail());
        if (exists) {
            log.warn("Register failed - email exists: {}", registerRequestAdmin.getEmail());
            throw new BadRequestException("Admin with email " + registerRequestAdmin.getEmail() + " already exists");
        }

        // Tạo company
        Company company = new Company();
        company.setName(registerRequestAdmin.getNameCompany());
        company.setAddress(registerRequestAdmin.getAddress());
        String companyCode = generateCompanyCode();
        company.setCompanyCode(companyCode);

        Company saved = companyRepository.save(company);

        // Chuyển đổi từ RegisterRequestAdmin sang Users
        Users user = AdminMapper.toUserEntity(registerRequestAdmin);
        user.setRole("Admin");
        user.setCompanyId(saved.getCompanyId());
        user.setCompanyCode(saved.getCompanyCode());
        String encodedPassword = passwordEncoder.encode(registerRequestAdmin.getPassword());
        user.setPassWord(encodedPassword);

        // Lưu vào database
        adminRepository.save(user);

        log.info("Admin registered successfully: {}", registerRequestAdmin.getEmail());

        // GỬI EMAIL THÔNG BÁO KÈM COMPANY CODE
        try {
            mailService.sendRegistrationSuccessEmail(
                    registerRequestAdmin.getEmail(),
                    companyCode,
                    registerRequestAdmin.getNameCompany(),
                    registerRequestAdmin.getNumberPhone()
            );
            log.info("Registration email sent to: {}", registerRequestAdmin.getEmail());
        } catch (Exception e) {
            log.error("Failed to send registration email, but user registered successfully: {}", e.getMessage());
            // Không throw exception để không ảnh hưởng đến việc đăng ký
        }
    }
    private String generateCompanyCode() {
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(6);

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(characters.length());
            code.append(characters.charAt(index));
        }
        return code.toString();
    }
}