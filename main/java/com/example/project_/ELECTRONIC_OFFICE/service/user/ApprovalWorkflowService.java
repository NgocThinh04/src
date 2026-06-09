package com.example.project_.ELECTRONIC_OFFICE.service.user;

import com.example.project_.ELECTRONIC_OFFICE.dto.EdgeInfo;
import com.example.project_.ELECTRONIC_OFFICE.dto.WorkflowStep;
import com.example.project_.ELECTRONIC_OFFICE.dto.request.ApprovalActionRequestDTO;
import com.example.project_.ELECTRONIC_OFFICE.dto.request.ApprovalRequestDTO;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.ApprovalActionDTO;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.ApprovalResponseDTO;
import com.example.project_.ELECTRONIC_OFFICE.entity.ApprovalAction;
import com.example.project_.ELECTRONIC_OFFICE.entity.ApprovalRequest;
import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import com.example.project_.ELECTRONIC_OFFICE.entity.Workflow;
import com.example.project_.ELECTRONIC_OFFICE.repository.admin.WorkflowRepository;
import com.example.project_.ELECTRONIC_OFFICE.repository.user.ApprovalActionRepository;
import com.example.project_.ELECTRONIC_OFFICE.repository.user.ApprovalRequestRepository;
import com.example.project_.ELECTRONIC_OFFICE.repository.user.UserRepository;
import com.example.project_.ELECTRONIC_OFFICE.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalWorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalActionRepository actionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ==================== UTILITY METHODS ====================

    private String generateRequestCode(UUID companyId) {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        String randomStr = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "REQ-" + dateStr + "-" + randomStr;
    }

    /**
     * Lấy workflow đang active cho company và requestType (name)
     */
    private Workflow getActiveWorkflow(UUID companyId, String requestType) {
        return workflowRepository
                .findByCompanyIdAndNameAndStatus(companyId, requestType, "active")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quy trình xét duyệt cho loại yêu cầu: " + requestType));
    }

    private List<Users> getApproversByRole(UUID companyId, String roleName) {
        List<Users> approvers = userRepository.findByCompanyIdAndPosition(companyId, roleName);

        log.info("📊 Found {} user(s) with position '{}' in company {}", approvers.size(), roleName, companyId);

        for (Users user : approvers) {
            log.info("  👤 User: id={}, name={}, position={}, email={}",
                    user.getUserId(),
                    user.getName(),
                    user.getPosition(),
                    user.getEmail());
        }

        return approvers;
    }

    // Helper method để clean JSON
    private String cleanJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return "[]";
        }

        String cleaned = jsonStr;

        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
            log.debug("Removed surrounding quotes");
        }

        if (cleaned.contains("\\\"")) {
            cleaned = cleaned.replace("\\\"", "\"");
            log.debug("Replaced escaped quotes");
        }

        return cleaned;
    }

    // ==================== PARSE WORKFLOW FROM JSON ====================

    private List<WorkflowStep> parseWorkflowSteps(JsonNode nodes, JsonNode edges) {
        log.info("========== PARSE WORKFLOW STEPS ==========");
        log.info("Total nodes: {}", nodes.size());
        log.info("Total edges: {}", edges.size());

        Map<String, JsonNode> nodeMap = new HashMap<>();
        Map<String, List<EdgeInfo>> adjacencyList = new HashMap<>();

        // Map nodes theo id
        for (JsonNode node : nodes) {
            String nodeId = node.get("id").asText();
            nodeMap.put(nodeId, node);
        }

        // Xây dựng đồ thị từ edges
        for (JsonNode edge : edges) {
            String source = edge.get("source").asText();
            String target = edge.get("target").asText();

            String edgeType = "APPROVE";
            String strokeColor = "#3b82f6";

            if (edge.has("style") && edge.get("style").has("stroke")) {
                strokeColor = edge.get("style").get("stroke").asText();
                if ("#ef4444".equals(strokeColor)) {
                    edgeType = "REJECT";
                }
            }

            EdgeInfo edgeInfo = EdgeInfo.builder()
                    .target(target)
                    .edgeType(edgeType)
                    .strokeColor(strokeColor)
                    .build();

            adjacencyList.computeIfAbsent(source, k -> new ArrayList<>()).add(edgeInfo);
        }

        // Tìm node START
        String startNodeId = null;
        for (Map.Entry<String, JsonNode> entry : nodeMap.entrySet()) {
            JsonNode node = entry.getValue();
            JsonNode data = node.get("data");
            if (data != null && data.has("label") && "START".equals(data.get("label").asText())) {
                startNodeId = entry.getKey();
                log.info("✅ FOUND START NODE: {}", startNodeId);
                break;
            }
        }

        if (startNodeId == null) {
            log.error("❌ START node not found!");
            throw new RuntimeException("Không tìm thấy node START trong quy trình");
        }

        // BFS để lấy thứ tự các APPROVAL node
        List<WorkflowStep> steps = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(startNodeId);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            if (visited.contains(currentId)) continue;
            visited.add(currentId);

            JsonNode currentNode = nodeMap.get(currentId);
            if (currentNode != null && currentNode.has("data")) {
                JsonNode data = currentNode.get("data");
                String label = data.has("label") ? data.get("label").asText() : "";
                String assignedRole = data.has("assignedRole") ? data.get("assignedRole").asText() : "";

                if ("APPROVAL".equals(label)) {
                    String approvalType = determineApprovalType(adjacencyList, currentId);
                    WorkflowStep step = WorkflowStep.builder()
                            .stepOrder(steps.size() + 1)
                            .stepName(assignedRole)
                            .assignedRole(assignedRole)
                            .approvalType(approvalType)
                            .build();
                    steps.add(step);
                    log.info("✅ Added step {}: role={}", steps.size(), assignedRole);
                }
            }

            List<EdgeInfo> nextEdges = adjacencyList.getOrDefault(currentId, new ArrayList<>());
            for (EdgeInfo edgeInfo : nextEdges) {
                if ("APPROVE".equals(edgeInfo.getEdgeType())) {
                    queue.add(edgeInfo.getTarget());
                }
            }
        }

        log.info("Total steps found: {}", steps.size());
        return steps;
    }

    private String determineApprovalType(Map<String, List<EdgeInfo>> adjacencyList, String nodeId) {
        List<EdgeInfo> edges = adjacencyList.getOrDefault(nodeId, new ArrayList<>());
        long approveEdgeCount = edges.stream()
                .filter(e -> "APPROVE".equals(e.getEdgeType()))
                .count();
        return approveEdgeCount > 1 ? "ALL" : "SINGLE";
    }

    // ==================== CONVERTERS ====================

    /**
     * Convert ApprovalAction entity → ApprovalActionDTO
     */
    private ApprovalActionDTO convertToActionDTO(ApprovalAction action, UUID currentUserId, UUID companyId) {
        boolean canApprove = false;
        if ("PENDING".equals(action.getAction())) {
            List<Users> allowedApprovers = getApproversByRole(companyId, action.getStepName());
            canApprove = allowedApprovers.stream()
                    .anyMatch(u -> u.getUserId().equals(currentUserId));
        }

        return ApprovalActionDTO.builder()
                .id(action.getId())
                .stepOrder(action.getStepOrder())
                .stepName(action.getStepName())
                .approvalType(action.getApprovalType())
                .action(action.getAction())
                .approverId(action.getApproverId())
                .approverName(action.getApproverName())
                .rejectionReason(action.getRejectionReason())
                .note(action.getNote())
                .approvedAt(action.getApprovedAt())
                .requestId(action.getRequestId())
                .canApprove(canApprove)
                .build();
    }

    /**
     * Convert ApprovalRequest entity → ApprovalResponseDTO
     */
    private ApprovalResponseDTO convertToResponseDTO(ApprovalRequest request, UUID currentUserId) {
        List<ApprovalAction> actions = actionRepository.findByRequestIdOrderByStepOrderAsc(request.getId());

        // Lấy thông tin người gửi
        String requesterName = "";
        try {
            Users requester = userRepository.findById(request.getRequesterId()).orElse(null);
            if (requester != null) {
                requesterName = requester.getName();
            }
        } catch (Exception e) {
            log.warn("Cannot get requester name for id: {}", request.getRequesterId());
        }

        List<ApprovalActionDTO> actionDTOs = actions.stream()
                .map(action -> convertToActionDTO(action, currentUserId, request.getCompanyId()))
                .collect(Collectors.toList());

        // Xác định bước hiện tại (step đang PENDING đầu tiên)
        Integer currentStepOrder = null;
        String currentStepName = null;
        for (ApprovalAction action : actions) {
            if ("PENDING".equals(action.getAction())) {
                currentStepOrder = action.getStepOrder();
                currentStepName = action.getStepName();
                break;
            }
        }

        return ApprovalResponseDTO.builder()
                .id(request.getId())
                .requestCode(request.getRequestCode())
                .title(request.getTitle())
                .description(request.getDescription())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .note(request.getNote())
                .requesterId(request.getRequesterId())
                .requesterName(requesterName)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .actions(actionDTOs)
                .currentStepOrder(currentStepOrder)
                .currentStepName(currentStepName)
                .isCompleted("APPROVED".equals(request.getStatus()) || "REJECTED".equals(request.getStatus()))
                .isPending("PENDING".equals(request.getStatus()))
                .build();
    }

    // ==================== CREATE APPROVAL REQUEST ====================

    @Transactional
    public ApprovalResponseDTO createApprovalRequest(UUID companyId, ApprovalRequestDTO dto) {
        log.info("Creating approval request for company: {}", companyId);

        // Lấy USER ID TỪ TOKEN
        UUID requesterId = jwtUtil.getUserIdFromToken(dto.getToken());
        log.info("Requester ID from token: {}", requesterId);

        // Kiểm tra người dùng tồn tại
        if (!userRepository.existsById(requesterId)) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }

        // Lấy workflow theo companyId và requestType (name)
        Workflow workflow = getActiveWorkflow(companyId, dto.getRequestType());
        log.info("Found workflow: {} (id: {})", workflow.getName(), workflow.getWorkflowId());

        try {
            String nodesStr = cleanJsonString(workflow.getNodes());
            String edgesStr = cleanJsonString(workflow.getEdges());

            JsonNode nodes = objectMapper.readTree(nodesStr);
            JsonNode edges = objectMapper.readTree(edgesStr);

            List<WorkflowStep> steps = parseWorkflowSteps(nodes, edges);

            if (steps.isEmpty()) {
                throw new RuntimeException("Quy trình không có bước APPROVAL nào");
            }

            // Tạo ApprovalRequest
            ApprovalRequest request = ApprovalRequest.builder()
                    .requestCode(generateRequestCode(companyId))
                    .companyId(companyId)
                    .requesterId(requesterId)
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .requestType(dto.getRequestType())
                    .status("PENDING")
                    .note(dto.getNote())
                    .build();

            request = requestRepository.save(request);
            log.info("✅ Approval request saved: id={}, code={}", request.getId(), request.getRequestCode());

            // Tạo ApprovalAction cho từng bước
            List<ApprovalAction> actions = new ArrayList<>();

            for (WorkflowStep step : steps) {
                List<Users> approvers = getApproversByRole(companyId, step.getAssignedRole());

                if (approvers.isEmpty()) {
                    log.warn("⚠️ No approver for role: {}", step.getAssignedRole());

                    ApprovalAction action = ApprovalAction.builder()
                            .requestId(request.getId())
                            .stepOrder(step.getStepOrder())
                            .stepName(step.getStepName())
                            .approvalType(step.getApprovalType())
                            .action("PENDING")
                            .build();
                    actions.add(action);

                } else {
                    for (Users approver : approvers) {
                        ApprovalAction action = ApprovalAction.builder()
                                .requestId(request.getId())
                                .stepOrder(step.getStepOrder())
                                .stepName(step.getStepName())
                                .approverId(approver.getUserId())
                                .approverName(approver.getName())
                                .approvalType(step.getApprovalType())
                                .action("PENDING")
                                .build();
                        actions.add(action);
                        log.info("✅ Created action for step {}: approver={}", step.getStepOrder(), approver.getName());
                    }
                }
            }

            actionRepository.saveAll(actions);
            log.info("✅ Saved {} approval actions", actions.size());

            return convertToResponseDTO(request, requesterId);

        } catch (Exception e) {
            log.error("Error creating approval request: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi xử lý quy trình: " + e.getMessage());
        }
    }

    // ==================== PROCESS APPROVAL (APPROVE/REJECT) ====================

    @Transactional
    public Map<String, Object> processApproval(UUID userId, ApprovalActionRequestDTO requestDTO) {
        log.info("Processing approval: userId={}, actionId={}, action={}",
                userId, requestDTO.getActionId(), requestDTO.getAction());

        UUID actionId = UUID.fromString(requestDTO.getActionId());
        ApprovalAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy action cần xử lý"));

        if (!"PENDING".equals(action.getAction())) {
            throw new RuntimeException("Action này đã được xử lý trước đó");
        }

        // Lấy thông tin request để biết companyId
        ApprovalRequest request = requestRepository.findById(action.getRequestId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        Users currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Kiểm tra user có đúng vai trò của step này không
        List<Users> allowedApprovers = getApproversByRole(request.getCompanyId(), action.getStepName());
        boolean isAllowed = allowedApprovers.stream().anyMatch(u -> u.getUserId().equals(userId));

        if (!isAllowed) {
            throw new RuntimeException("Bạn không có quyền duyệt yêu cầu này");
        }

        // Cập nhật action
        action.setApproverId(userId);
        action.setApproverName(currentUser.getName());
        action.setAction(requestDTO.getAction());
        action.setApprovedAt(OffsetDateTime.now());
        action.setNote(requestDTO.getNote());

        if ("REJECTED".equals(requestDTO.getAction())) {
            action.setRejectionReason(requestDTO.getRejectionReason());
        }

        actionRepository.save(action);

        if ("REJECTED".equals(requestDTO.getAction())) {
            return handleRejection(action, request);
        } else {
            return handleApproval(action, request);
        }
    }

    private Map<String, Object> handleApproval(ApprovalAction action, ApprovalRequest request) {
        UUID requestId = action.getRequestId();
        Integer currentStepOrder = action.getStepOrder();
        String approvalType = action.getApprovalType();

        boolean isStepCompleted = checkStepCompleted(requestId, currentStepOrder, approvalType);

        if (!isStepCompleted) {
            return Map.of(
                    "message", "Đã ghi nhận duyệt. Chờ thêm người duyệt khác trong bước này.",
                    "status", "PROCESSING",
                    "requestId", requestId,
                    "currentStep", currentStepOrder
            );
        }

        return moveToNextStep(requestId, currentStepOrder, request);
    }

    private Map<String, Object> handleRejection(ApprovalAction action, ApprovalRequest request) {
        UUID requestId = action.getRequestId();

        request.setStatus("REJECTED");
        request.setUpdatedAt(OffsetDateTime.now());
        requestRepository.save(request);

        // Hủy tất cả các action đang PENDING khác
        List<ApprovalAction> pendingActions = actionRepository.findByRequestIdAndAction(requestId, "PENDING");
        for (ApprovalAction pendingAction : pendingActions) {
            pendingAction.setAction("CANCELLED");
            actionRepository.save(pendingAction);
        }

        return Map.of(
                "message", "Yêu cầu đã bị từ chối",
                "status", "REJECTED",
                "requestId", requestId,
                "rejectedBy", action.getApproverName(),
                "rejectionReason", action.getRejectionReason()
        );
    }

    private boolean checkStepCompleted(UUID requestId, Integer stepOrder, String approvalType) {
        List<ApprovalAction> actionsInStep = actionRepository.findByRequestIdAndStepOrder(requestId, stepOrder);

        if ("SINGLE".equals(approvalType)) {
            long approvedCount = actionsInStep.stream()
                    .filter(a -> "APPROVED".equals(a.getAction()))
                    .count();
            return approvedCount >= 1;
        } else {
            long totalCount = actionsInStep.size();
            long approvedCount = actionsInStep.stream()
                    .filter(a -> "APPROVED".equals(a.getAction()))
                    .count();
            return totalCount > 0 && approvedCount == totalCount;
        }
    }

    private Map<String, Object> moveToNextStep(UUID requestId, Integer currentStepOrder, ApprovalRequest request) {
        List<ApprovalAction> nextActions = actionRepository
                .findByRequestIdAndStepOrderGreaterThanAndAction(requestId, currentStepOrder, "PENDING");

        if (nextActions.isEmpty()) {
            request.setStatus("APPROVED");
            request.setUpdatedAt(OffsetDateTime.now());
            requestRepository.save(request);

            return Map.of(
                    "message", "Yêu cầu đã được duyệt hoàn tất!",
                    "status", "APPROVED",
                    "requestId", requestId
            );
        }

        Integer nextStepOrder = nextActions.get(0).getStepOrder();

        return Map.of(
                "message", "Đã duyệt thành công. Chuyển sang bước tiếp theo.",
                "status", "IN_PROGRESS",
                "requestId", requestId,
                "currentStep", currentStepOrder,
                "nextStep", nextStepOrder
        );
    }

    // ==================== QUERY METHODS ====================

    /**
     * Lấy danh sách yêu cầu cần duyệt cho người dùng
     */
    public List<ApprovalResponseDTO> getPendingRequestsForUser(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        log.info("Getting all requests for user: {} (position: {}, companyId: {})",
                user.getName(), user.getPosition(), user.getCompanyId());

        // Lấy tất cả yêu cầu mà user này là approver (theo position)
        // Không phân biệt trạng thái PENDING/APPROVED/REJECTED
        // Và không lấy yêu cầu do chính user này gửi
        List<ApprovalRequest> requests = requestRepository.findRequestsByApproverRole(
                user.getCompanyId(),
                user.getPosition(),
                userId  // Thêm tham số để loại trừ yêu cầu do chính user gửi
        );

        log.info("Found {} requests for approver role: {}", requests.size(), user.getPosition());

        return requests.stream()
                .map(request -> convertToResponseDTO(request, userId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một yêu cầu
     */
    public ApprovalResponseDTO getRequestDetail(UUID requestId, UUID currentUserId) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));
        return convertToResponseDTO(request, currentUserId);
    }

    /**
     * Lấy danh sách yêu cầu do tôi gửi
     */
    public List<ApprovalResponseDTO> getMyRequests(UUID requesterId) {
        List<ApprovalRequest> requests = requestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
        return requests.stream()
                .map(request -> convertToResponseDTO(request, requesterId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách yêu cầu của công ty (cho Admin)
     */
    public List<ApprovalResponseDTO> getCompanyRequests(UUID companyId, String status, UUID currentUserId) {
        List<ApprovalRequest> requests;
        if (status != null && !status.isEmpty()) {
            requests = requestRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status);
        } else {
            requests = requestRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        }
        return requests.stream()
                .map(request -> convertToResponseDTO(request, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả workflow của công ty
     */
    public List<Workflow> getWorkflowsByCompany(UUID companyId) {
        return workflowRepository.findByCompanyIdAndStatusOrderByCreateAtDesc(companyId, "active");
    }

    /**
     * Lấy workflow đang active theo companyId và name
     */
    public Workflow getActiveWorkflowByCompanyAndName(UUID companyId, String name) {
        return getActiveWorkflow(companyId, name);
    }
}