package com.example.project_.ELECTRONIC_OFFICE.controller.admin;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.PositionRequest;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.PositionResponse;
import com.example.project_.ELECTRONIC_OFFICE.service.admin.PositionService;
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
@RequestMapping("/api/positions")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class PositionController {

    private final PositionService positionService;

    // Lấy tất cả chức vụ theo companyId
    @GetMapping
    public ResponseEntity<?> getAllPositions(@RequestParam(required = false) UUID companyId) {
        log.info("GET /api/admin/positions - companyId: {}", companyId);

        try {
            List<PositionResponse> positions = positionService.getAllPositions(companyId);
            return ResponseEntity.ok(positions);
        } catch (Exception e) {
            log.error("Error getting positions: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Lấy chức vụ theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPositionById(@PathVariable UUID id) {
        log.info("GET /api/admin/positions/{}", id);

        try {
            PositionResponse position = positionService.getPositionById(id);
            return ResponseEntity.ok(position);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // Tạo chức vụ mới
    @PostMapping
    public ResponseEntity<?> createPosition(@RequestBody PositionRequest request) {
        log.info("POST /api/admin/positions - name: {}", request.getPositionName());

        try {
            if (request.getPositionName() == null || request.getPositionName().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên chức vụ không được để trống");
                return ResponseEntity.badRequest().body(error);
            }

            if (request.getCompanyId() == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Company ID không được để trống");
                return ResponseEntity.badRequest().body(error);
            }

            PositionResponse created = positionService.createPosition(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Cập nhật chức vụ
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePosition(@PathVariable UUID id, @RequestBody PositionRequest request) {
        log.info("PUT /api/positions/{}", id);

        try {
            PositionResponse updated = positionService.updatePosition(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // Xóa chức vụ
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePosition(@PathVariable UUID id) {
        log.info("DELETE /api/admin/positions/{}", id);

        try {
            positionService.deletePosition(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Position deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // Cập nhật status chức vụ
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updatePositionStatus(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        log.info("PATCH /api/admin/positions/{}/status", id);

        try {
            String status = payload.get("status");
            if (status == null || (!status.equals("ACTIVE") && !status.equals("INACTIVE"))) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Trạng thái không hợp lệ. Chấp nhận: ACTIVE, INACTIVE");
                return ResponseEntity.badRequest().body(error);
            }

            PositionResponse updated = positionService.updatePositionStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
