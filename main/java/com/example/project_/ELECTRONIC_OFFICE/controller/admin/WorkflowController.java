package com.example.project_.ELECTRONIC_OFFICE.controller.admin;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.WorkflowRequest;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.WorkflowResponse;
import com.example.project_.ELECTRONIC_OFFICE.service.admin.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    public ResponseEntity<?> getAllWorkflows(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/workflows - Get all workflows for companyId: {}", companyId);

        try {
            List<WorkflowResponse> workflows;
            if (companyId != null) {
                workflows = workflowService.getWorkflowsByCompanyId(companyId);
            } else {
                workflows = workflowService.getAllWorkflows();
            }
            if (workflows != null && !workflows.isEmpty()) {
                log.info("📋 DETAILED WORKFLOW LIST:");
                for (int i = 0; i < workflows.size(); i++) {
                    WorkflowResponse wf = workflows.get(i);
                    log.info("--- Workflow {} ---", i + 1);
                    log.info("  workflowId: {}", wf.getWorkflowId());
                    log.info("  name: {}", wf.getName());
                    log.info("  description: {}", wf.getDescription());
                    log.info("  status: {}", wf.getStatus());
                    log.info("  version: {}", wf.getVersion());
                    log.info("  createdBy: {}", wf.getCreatedBy());
                    log.info("  createdAt: {}", wf.getCreatedAt());
                    log.info("  updatedAt: {}", wf.getUpdatedAt());
                    log.info("  nodes: {}", wf.getNodes());
                    log.info("  nodes type: {}", wf.getNodes() != null ? wf.getNodes().getClass().getName() : "null");
                    log.info("  edges: {}", wf.getEdges());
                    log.info("  edges type: {}", wf.getEdges() != null ? wf.getEdges().getClass().getName() : "null");
                }
            } else {
                log.warn("⚠️ No workflows found!");
            }

            log.info("✅ Returning {} workflows to FE", workflows != null ? workflows.size() : 0);
            log.info("======================================");

            return ResponseEntity.ok(workflows);
        } catch (Exception e) {
            log.error("Error getting workflows: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkflowById(@PathVariable UUID id) {
        log.info("GET /api/workflows/{} - Get workflow by id", id);
        try {
            WorkflowResponse workflow = workflowService.getWorkflowById(id);
            return ResponseEntity.ok(workflow);
        } catch (RuntimeException e) {
            log.error("Workflow not found: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }


    @PostMapping
    public ResponseEntity<?> createWorkflow(@RequestBody WorkflowRequest workflowRequest) {
        log.info("POST /api/workflows - Create new workflow: {}", workflowRequest.getName());
        System.out.println(workflowRequest);
        try {

            if (workflowRequest.getName() == null || workflowRequest.getName().trim().isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Tên workflow không được để trống");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            WorkflowResponse created = workflowService.createWorkflow(workflowRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (RuntimeException e) {
            log.error("Error creating workflow: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error creating workflow: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể tạo workflow. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkflow(@PathVariable UUID id, @RequestBody WorkflowRequest workflowRequest) {
        log.info("PUT /api/workflows/{} - Update workflow", id);

        try {
            // Validate dữ liệu đầu vào
            if (workflowRequest.getName() == null || workflowRequest.getName().trim().isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Tên workflow không được để trống");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            WorkflowResponse updated = workflowService.updateWorkflow(id, workflowRequest);
            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            log.error("Error updating workflow: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error updating workflow: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể cập nhật workflow. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkflow(@PathVariable UUID id) {
        log.info("DELETE /api/workflows/{} - Delete workflow", id);

        try {
            workflowService.deleteWorkflow(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Workflow deleted successfully");
            response.put("id", id.toString());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error deleting workflow: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error deleting workflow: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể xóa workflow. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        log.info("PATCH /api/workflows/{}/status - Update status to: {}", id, payload.get("status"));

        try {
            String status = payload.get("status");
            if (status == null || (!status.equals("draft") && !status.equals("active") && !status.equals("inactive"))) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Trạng thái không hợp lệ. Chấp nhận: draft, active, inactive");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            System.out.println("Trang thai:"+status);
            WorkflowResponse updated = workflowService.updateStatus(id, status);
            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            log.error("Error updating status: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error updating status: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể cập nhật trạng thái. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PostMapping("/{id}/duplicate")
    public ResponseEntity<?> duplicateWorkflow(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload) {
        log.info("POST /api/workflows/{}/duplicate - Duplicate workflow", id);

        try {
            if (payload == null) {
                payload = new HashMap<>();
            }

            WorkflowResponse duplicated = workflowService.duplicateWorkflow(id, payload);
            return ResponseEntity.status(HttpStatus.CREATED).body(duplicated);

        } catch (RuntimeException e) {
            log.error("Error duplicating workflow: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error duplicating workflow: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể sao chép workflow. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateWorkflow(@RequestBody WorkflowRequest workflowRequest) {
        log.info("POST /api/workflows/validate - Validate workflow: {}", workflowRequest.getName());

        try {
            boolean isValid = workflowService.validateWorkflow(workflowRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("message", isValid ? "Workflow hợp lệ" : "Workflow không hợp lệ");

            if (!isValid) {
                List<String> errors = new ArrayList<>();
                if (workflowRequest.getName() == null || workflowRequest.getName().trim().isEmpty()) {
                    errors.add("Tên workflow không được để trống");
                }
                if (workflowRequest.getNodes() == null || workflowRequest.getEdges() == null) {
                    errors.add("Nodes và edges không được để trống");
                }
                response.put("errors", errors);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating workflow: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể validate workflow. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}