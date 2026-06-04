package com.example.project_.ELECTRONIC_OFFICE.service.admin;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.UserRequest;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.UserResponse;
import com.example.project_.ELECTRONIC_OFFICE.entity.Company;
import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import com.example.project_.ELECTRONIC_OFFICE.exception.CannotDeleteAdminException;
import com.example.project_.ELECTRONIC_OFFICE.exception.CannotUpdateAdminException;
import com.example.project_.ELECTRONIC_OFFICE.mapper.UserMapper;
import com.example.project_.ELECTRONIC_OFFICE.repository.admin.CompanyRepository;
import com.example.project_.ELECTRONIC_OFFICE.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CompanyRepository companyRepository;
    /**
     * Lấy tất cả users theo companyId
     */
    public List<UserResponse> getAllUsers(UUID companyId) {
        log.info("Getting all users for companyId: {}", companyId);

        List<Users> users;
        if (companyId != null) {
            users = userRepository.findByCompanyIdOrderByCreateAtDesc(companyId);
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy user theo ID
     */
    public UserResponse getUserById(UUID userId) {
        log.info("Getting user by id: {}", userId);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return userMapper.toResponse(user);
    }

    /**
     * Tạo user mới
     */
    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("Creating new user: {}", request.getEmail());

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        // Kiểm tra username đã tồn tại
        if (request.getUserName() != null && userRepository.existsByUserName(request.getUserName())) {
            throw new RuntimeException("Username already exists: " + request.getUserName());
        }

        Users user = userMapper.toEntity(request);
        Company companyCode = companyRepository.findByCompanyId(request.getCompanyId());
        user.setCompanyCode(companyCode.getCompanyCode());
        // Set giá trị mặc định
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }
        if (user.getRole() == null) {
            user.setRole("User");
        }

        Users saved = userRepository.save(user);
        log.info("User created successfully with id: {}", saved.getUserId());

        return userMapper.toResponse(saved);
    }

    /**
     * Cập nhật user
     */
    @Transactional
    public UserResponse updateUser(UUID userId, UserRequest request) {
        log.info("Updating user: {}", userId);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Kiểm tra nếu user có role là ADMIN thì không cho cập nhật
        if (user.getRole() != null && user.getRole().equals("Admin")) {
            log.warn("Cannot update admin user: {}", userId);
            throw new CannotUpdateAdminException("Không thể cập nhật thông tin của tài khoản ADMIN");
        }

        userMapper.updateEntity(request, user);

        Users saved = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        return userMapper.toResponse(saved);
    }

    /**
     * Xóa user
     */
    @Transactional
    public void deleteUser(UUID userId) {
        log.info("Deleting user: {}", userId);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Kiểm tra nếu user có role là ADMIN thì không cho xóa
        if (user.getRole() != null && user.getRole().equals("Admin")) {
            log.warn("Cannot delete admin user: {}", userId);
            throw new CannotDeleteAdminException("Không thể xóa tài khoản ADMIN. Vui lòng liên hệ quản trị viên cấp cao.");
        }

        userRepository.delete(user);
        log.info("User deleted successfully: {}", userId);
    }
    /**
     * Cập nhật trạng thái user
     */
    @Transactional
    public UserResponse updateUserStatus(UUID userId, String status) {
        log.info("Updating user status: {} to {}", userId, status);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Kiểm tra nếu user có role là ADMIN thì không cho cập nhật status
        // Dùng giống hệt deleteUser
        if (user.getRole() != null && user.getRole().equals("Admin")) {
            log.warn("Cannot update status for admin user: {}", userId);
            throw new CannotUpdateAdminException("Không thể cập nhật trạng thái của tài khoản ADMIN");
        }

        user.setStatus(status);
        Users saved = userRepository.save(user);

        return userMapper.toResponse(saved);
    }

    /**
     * Lấy active users theo companyId
     */
    public List<UserResponse> getActiveUsersByCompanyId(UUID companyId) {
        log.info("Getting active users for companyId: {}", companyId);

        List<Users> users = userRepository.findActiveUsersByCompanyId(companyId);

        return users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }
}