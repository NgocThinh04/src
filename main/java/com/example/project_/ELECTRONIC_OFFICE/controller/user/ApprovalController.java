package com.example.project_.ELECTRONIC_OFFICE.controller.user;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.ApprovalActionRequestDTO;
import com.example.project_.ELECTRONIC_OFFICE.dto.request.ApprovalRequestDTO;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.ApprovalResponseDTO;
import com.example.project_.ELECTRONIC_OFFICE.entity.Workflow;
import com.example.project_.ELECTRONIC_OFFICE.service.user.ApprovalWorkflowService;
import com.example.project_.ELECTRONIC_OFFICE.util.JwtUtil;
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
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {

    private final ApprovalWorkflowService approvalWorkflowService;
    private final JwtUtil jwtUtil;
    // ==================== CREATE REQUEST ====================

    /**
     * Gửi yêu cầu duyệt mới
     * POST /api/approvals/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitRequest(@RequestBody ApprovalRequestDTO requestDTO) {
        log.info("POST /api/approvals/submit - request: {}", requestDTO);

        try {
            UUID companyId = UUID.fromString(requestDTO.getCompanyId());
            ApprovalResponseDTO created = approvalWorkflowService.createApprovalRequest(companyId, requestDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Yêu cầu đã được gửi thành công");
            response.put("requestId", created.getId());
            response.put("requestCode", created.getRequestCode());
            response.put("status", created.getStatus());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Dữ liệu không hợp lệ");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error creating approval request: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== PROCESS APPROVAL ====================

    /**
     * Xử lý duyệt (approve/reject)
     * POST /api/approvals/process
     */
    @PostMapping("/process")
    public ResponseEntity<?> processApproval(@RequestBody ApprovalActionRequestDTO requestDTO,
                                             @RequestHeader("X-User-Id") String userId) {
        log.info("POST /api/approvals/process - userId: {}, actionId: {}, action: {}",
                userId, requestDTO.getActionId(), requestDTO.getAction());

        try {
            UUID userUuid = UUID.fromString(userId);
            Map<String, Object> result = approvalWorkflowService.processApproval(userUuid, requestDTO);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Dữ liệu không hợp lệ");
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            log.error("Error processing approval: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Không thể xử lý yêu cầu. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ==================== QUERY REQUESTS ====================

    /**
     * Lấy danh sách yêu cầu cần tôi duyệt
     * GET /api/approvals/pending?userId={userId}
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests(@RequestHeader("Authorization") String authHeader) {
        log.info("GET /api/approvals/pending");

        try {
            // Lấy userId từ token
            UUID userId = jwtUtil.getUserIdFromToken(authHeader);
            log.info("User ID from token: {}", userId);

            List<ApprovalResponseDTO> pendingRequests = approvalWorkflowService.getPendingRequestsForUser(userId);
            return ResponseEntity.ok(pendingRequests);

        } catch (Exception e) {
            log.error("Error getting pending requests: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Lấy chi tiết một yêu cầu
     * GET /api/approvals/{requestId}?userId={userId}
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getRequestDetail(@PathVariable String requestId,
                                              @RequestParam String userId) {
        log.info("GET /api/approvals/{} - userId: {}", requestId, userId);

        try {
            UUID requestUuid = UUID.fromString(requestId);
            UUID userUuid = UUID.fromString(userId);
            ApprovalResponseDTO request = approvalWorkflowService.getRequestDetail(requestUuid, userUuid);
            return ResponseEntity.ok(request);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Dữ liệu không hợp lệ");
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            log.error("Request not found: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error getting request detail: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Không thể lấy thông tin yêu cầu");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Lấy danh sách yêu cầu do tôi gửi
     * GET /api/approvals/my-requests?requesterId={requesterId}
     */
    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests(@RequestParam String requesterId) {
        log.info("GET /api/approvals/my-requests - requesterId: {}", requesterId);

        try {
            UUID requesterUuid = UUID.fromString(requesterId);
            List<ApprovalResponseDTO> myRequests = approvalWorkflowService.getMyRequests(requesterUuid);
            return ResponseEntity.ok(myRequests);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Dữ liệu không hợp lệ");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error getting my requests: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Lấy danh sách yêu cầu của công ty (cho Admin/Manager)
     * GET /api/approvals/company?companyId={companyId}&status={status}&userId={userId}
     */
    @GetMapping("/company")
    public ResponseEntity<?> getCompanyRequests(@RequestParam String companyId,
                                                @RequestParam(required = false) String status,
                                                @RequestParam String userId) {
        log.info("GET /api/approvals/company - companyId: {}, status: {}, userId: {}", companyId, status, userId);

        try {
            UUID companyUuid = UUID.fromString(companyId);
            UUID userUuid = UUID.fromString(userId);
            List<ApprovalResponseDTO> requests = approvalWorkflowService.getCompanyRequests(companyUuid, status, userUuid);
            return ResponseEntity.ok(requests);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Dữ liệu không hợp lệ");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error getting company requests: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== WORKFLOW MANAGEMENT ====================

    /**
     * Lấy tất cả workflow của công ty
     * GET /api/approvals/workflows?companyId={companyId}
     */
    @GetMapping("/workflows")
    public ResponseEntity<?> getWorkflowsByCompany(@RequestParam String companyId) {
        log.info("GET /api/approvals/workflows - companyId: {}", companyId);

        try {
            UUID companyUuid = UUID.fromString(companyId);
            List<Workflow> workflows = approvalWorkflowService.getWorkflowsByCompany(companyUuid);
            return ResponseEntity.ok(workflows);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Dữ liệu không hợp lệ");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error getting workflows: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Lấy workflow đang active theo companyId và name
     * GET /api/approvals/workflows/active?companyId={companyId}&name={name}
     */
    @GetMapping("/workflows/active")
    public ResponseEntity<?> getActiveWorkflow(@RequestParam String companyId,
                                               @RequestParam String name) {
        log.info("GET /api/approvals/workflows/active - companyId: {}, name: {}", companyId, name);

        try {
            UUID companyUuid = UUID.fromString(companyId);
            Workflow workflow = approvalWorkflowService.getActiveWorkflowByCompanyAndName(companyUuid, name);
            return ResponseEntity.ok(workflow);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Dữ liệu không hợp lệ");
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            log.error("Workflow not found: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error getting active workflow: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}