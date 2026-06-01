package com.example.project_.ELECTRONIC_OFFICE.service.admin;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.WorkflowRequest;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.WorkflowResponse;
import com.example.project_.ELECTRONIC_OFFICE.entity.Workflow;
import com.example.project_.ELECTRONIC_OFFICE.mapper.WorkflowMapper;
import com.example.project_.ELECTRONIC_OFFICE.repository.admin.WorkflowRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowMapper workflowMapper;

    // Lấy tất cả workflows
    public List<WorkflowResponse> getAllWorkflows() {
        log.info("Getting all workflows");
        return workflowRepository.findAll().stream()
                .map(workflowMapper::toReponse)
                .collect(Collectors.toList());
    }

    // Lấy workflows theo companyId
    public List<WorkflowResponse> getWorkflowsByCompanyId(UUID companyId) {
        log.info("Getting workflows by companyId: {}", companyId);
        return workflowRepository.findByCompanyId(companyId).stream()
                .map(workflowMapper::toReponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy workflow theo ID
     */
    public WorkflowResponse getWorkflowById(UUID workflowId) {
        log.info("Getting workflow by id: {}", workflowId);
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + workflowId));
        return workflowMapper.toReponse(workflow);  // Sử dụng mapper
    }

    /**
     * Tạo mới workflow
     */
    @Transactional
    public WorkflowResponse createWorkflow(WorkflowRequest workflowRequest) {
        log.info("Creating new workflow: {}", workflowRequest.getName());

        // Kiểm tra tên trùng
//        if (workflowRepository.existsByName(workflowRequest.getName())) {
//            throw new RuntimeException("Workflow name already exists: " + workflowRequest.getName());
//        }

        // Chuyển DTO sang Entity bằng mapper
        Workflow workflow = workflowMapper.toEntity(workflowRequest);

        // Set các giá trị mặc định nếu chưa có
        if (workflow.getStatus() == null) {
            workflow.setStatus("draft");
        }
        if (workflow.getVersion() == null) {
            workflow.setVersion(1);
        }
        if (workflow.getCreateBy() == null) {
            workflow.setCreateBy("system");
        }

        Workflow saved = workflowRepository.save(workflow);
        log.info("Workflow created successfully with id: {}", saved.getWorkflowId());

        return workflowMapper.toReponse(saved);  // Sử dụng mapper
    }

    /**
     * Cập nhật workflow
     */
    @Transactional
    public WorkflowResponse updateWorkflow(UUID id, WorkflowRequest workflowRequest) {
        log.info("Updating workflow: {}", id);

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));

        // Cập nhật entity từ DTO bằng mapper
        workflowMapper.updateEntity(workflowRequest, workflow);

        Workflow saved = workflowRepository.save(workflow);
        log.info("Workflow updated successfully: {}", id);

        return workflowMapper.toReponse(saved);  // Sử dụng mapper
    }

    /**
     * Xóa workflow
     */
    @Transactional
    public void deleteWorkflow(UUID id) {
        log.info("Deleting workflow: {}", id);

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));

        workflowRepository.delete(workflow);
        log.info("Workflow deleted successfully: {}", id);
    }

    /**
     * Cập nhật trạng thái workflow
     */
    @Transactional
    public WorkflowResponse updateStatus(UUID id, String status) {
        log.info("Updating workflow status: {} to {}", id, status);

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));

        workflow.setStatus(status);
        workflow.setUpdateAt(OffsetDateTime.now());
        Workflow saved = workflowRepository.save(workflow);

        return workflowMapper.toReponse(saved);  // Sử dụng mapper
    }

    /**
     * Sao chép workflow
     */
    @Transactional
    public WorkflowResponse duplicateWorkflow(UUID id, Map<String, String> payload) {
        log.info("Duplicating workflow: {}", id);

        Workflow original = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));

        String newName = payload.getOrDefault("name", original.getName() + " (Copy)");

        Workflow duplicate = new Workflow();
        duplicate.setName(newName);
        duplicate.setDescription("Sao chép từ: " + original.getName());
        duplicate.setNodes(original.getNodes());
        duplicate.setEdges(original.getEdges());
        duplicate.setStatus("draft");
        duplicate.setVersion(1);
        duplicate.setCreateBy("system");

        Workflow saved = workflowRepository.save(duplicate);
        log.info("Workflow duplicated successfully: {} -> {}", id, saved.getWorkflowId());

        return workflowMapper.toReponse(saved);  // Sử dụng mapper
    }

    /**
     * Validate workflow
     */
    public boolean validateWorkflow(WorkflowRequest workflowRequest) {
        // Kiểm tra cơ bản
        if (workflowRequest.getName() == null || workflowRequest.getName().trim().isEmpty()) {
            log.warn("Validation failed: Workflow name is empty");
            return false;
        }

        // Kiểm tra nodes và edges
        if (workflowRequest.getNodes() == null || workflowRequest.getEdges() == null) {
            log.warn("Validation failed: Nodes or edges is null");
            return false;
        }

        log.info("Workflow validation passed: {}", workflowRequest.getName());
        return true;
    }
}