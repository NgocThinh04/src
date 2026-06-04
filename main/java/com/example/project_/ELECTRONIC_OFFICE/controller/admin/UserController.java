package com.example.project_.ELECTRONIC_OFFICE.controller.admin;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.UserRequest;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.UserResponse;
import com.example.project_.ELECTRONIC_OFFICE.exception.CannotDeleteAdminException;
import com.example.project_.ELECTRONIC_OFFICE.exception.CannotUpdateAdminException;
import com.example.project_.ELECTRONIC_OFFICE.service.admin.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Lấy tất cả users theo companyId
     * GET /api/admin/users?companyId={companyId}
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestParam(required = false) UUID companyId) {
        log.info("GET /api/users - companyId: {}", companyId);

        try {
            List<UserResponse> users = userService.getAllUsers(companyId);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error getting users: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Lấy user theo ID
     * GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        log.info("GET /api/users/{}", id);

        try {
            UserResponse user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Tạo user mới
     * POST /api/admin/users
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserRequest request) {
        log.info("POST /api/users - email: {}", request.getEmail());

        try {
            // Validate dữ liệu đầu vào
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Email không được để trống");
                return ResponseEntity.badRequest().body(error);
            }

            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Mật khẩu không được để trống");
                return ResponseEntity.badRequest().body(error);
            }

            UserResponse created = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("Unexpected error creating user: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Không thể tạo người dùng. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Cập nhật user
     * PUT /api/admin/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserRequest request) {
        log.info("PUT /api/users/{}", id);

        try {
            UserResponse updated = userService.updateUser(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Unexpected error updating user: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Không thể cập nhật người dùng. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Xóa user
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        log.info("DELETE /api/users/{}", id);

        try {
            userService.deleteUser(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            response.put("id", id.toString());
            return ResponseEntity.ok(response);
        } catch (CannotDeleteAdminException e) {
            // Bắt lỗi không thể xóa ADMIN
            log.warn("Cannot delete admin user: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (RuntimeException e) {
            // Bắt lỗi user not found
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            // Bắt các lỗi khác
            log.error("Unexpected error deleting user: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Không thể xóa người dùng. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Cập nhật trạng thái user
     * PATCH /api/admin/users/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        log.info("PATCH /api/users/{}/status", id);

        try {
            String status = payload.get("status");
            if (status == null || (!status.equals("ACTIVE") && !status.equals("INACTIVE"))) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Trạng thái không hợp lệ. Chấp nhận: ACTIVE, INACTIVE");
                return ResponseEntity.badRequest().body(error);
            }

            UserResponse updated = userService.updateUserStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (CannotUpdateAdminException e) {
            // Bắt lỗi không thể cập nhật ADMIN
            log.warn("Cannot update admin user status: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Unexpected error updating user status: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Không thể cập nhật trạng thái. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Lấy active users theo companyId
     * GET /api/admin/users/active?companyId={companyId}
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveUsers(@RequestParam(required = false) UUID companyId) {
        log.info("GET /api/users/active - companyId: {}", companyId);

        try {
            List<UserResponse> users = userService.getActiveUsersByCompanyId(companyId);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}