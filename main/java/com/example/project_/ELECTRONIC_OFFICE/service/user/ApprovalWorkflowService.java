package com.example.project_.ELECTRONIC_OFFICE.service.user;

import com.example.project_.ELECTRONIC_OFFICE.dto.EdgeInfo;
import com.example.project_.ELECTRONIC_OFFICE.dto.StepStatus;
import com.example.project_.ELECTRONIC_OFFICE.dto.WorkflowStep;
import com.example.project_.ELECTRONIC_OFFICE.dto.request.ApprovalActionRequestDTO;
import com.example.project_.ELECTRONIC_OFFICE.dto.request.ApprovalRequestDTO;
import com.example.project_.ELECTRONIC_OFFICE.dto.request.UpdateApprovalRequestDTO;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.ApprovalActionDTO;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.ApprovalDetail;
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

    private Workflow getActiveWorkflow(UUID companyId, String requestType) {
        return workflowRepository
                .findByCompanyIdAndNameAndStatus(companyId, requestType, "active")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quy trình xét duyệt cho loại yêu cầu: " + requestType));
    }

    private List<Users> getApproversByRole(UUID companyId, String roleName) {
        List<Users> approvers = userRepository.findByCompanyIdAndPosition(companyId, roleName);
        log.info("📊 Found {} user(s) with position '{}' in company {}", approvers.size(), roleName, companyId);
        for (Users user : approvers) {
            log.info("  👤 User: id={}, name={}, position={}", user.getUserId(), user.getName(), user.getPosition());
        }
        return approvers;
    }

    private String cleanJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return "[]";
        }

        String cleaned = jsonStr;

        // Loại bỏ quotes ở đầu và cuối nếu có
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
            log.debug("Removed surrounding quotes");
        }

        // Giải mã escape characters
        if (cleaned.contains("\\\"")) {
            cleaned = cleaned.replace("\\\"", "\"");
            log.debug("Replaced escaped quotes");
        }

        if (cleaned.contains("\\\\")) {
            cleaned = cleaned.replace("\\\\", "\\");
            log.debug("Replaced escaped backslashes");
        }

        return cleaned;
    }

    // ==================== PARSE WORKFLOW FROM JSON ====================
    private List<WorkflowStep> parseWorkflowSteps(JsonNode nodes, JsonNode edges,
                                                  Map<String, String> approvalActions,
                                                  Map<String, ApprovalDetail> approvalDetails) {
        List<WorkflowStep> steps = new ArrayList<>();
        log.info("=== PARSE WORKFLOW STEPS ===");

        Map<String, JsonNode> nodeMap = new HashMap<>();
        for (JsonNode node : nodes) {
            nodeMap.put(node.get("id").asText(), node);
        }

        // Build edges map
        Map<String, List<EdgeInfo>> edgesMap = new HashMap<>();
        for (JsonNode edge : edges) {
            String source = edge.get("source").asText();
            String target = edge.get("target").asText();
            String edgeType = "conditional";
            String label = "";

            if (edge.has("data")) {
                JsonNode data = edge.get("data");
                if (data.has("type")) {
                    edgeType = data.get("type").asText();
                }
                if (data.has("label")) {
                    label = data.get("label").asText();
                }
            }

            EdgeInfo edgeInfo = EdgeInfo.builder()
                    .target(target)
                    .edgeType(edgeType)
                    .label(label)
                    .build();

            edgesMap.computeIfAbsent(source, k -> new ArrayList<>()).add(edgeInfo);
            log.debug("Added edge: {} -> {} [type={}]", source, target, edgeType);
        }

        // Log all edges
        log.info("=== ALL EDGES ===");
        for (Map.Entry<String, List<EdgeInfo>> entry : edgesMap.entrySet()) {
            for (EdgeInfo ei : entry.getValue()) {
                log.info("Edge: {} -> {} [{}]", entry.getKey(), ei.getTarget(), ei.getEdgeType());
            }
        }

        // Find START node
        String startNodeId = findStartNodeId(nodes);
        if (startNodeId == null) {
            throw new RuntimeException("Không tìm thấy node START");
        }
        log.info("START node: {}", startNodeId);

        // Traverse workflow
        String currentNodeId = startNodeId;
        int stepOrder = 1;
        Set<String> visited = new HashSet<>();
        int maxDepth = 50;
        int currentDepth = 0;

        while (currentNodeId != null && currentDepth++ < maxDepth) {
            if (visited.contains(currentNodeId)) {
                log.warn("Loop detected at node: {}", currentNodeId);
                break;
            }
            visited.add(currentNodeId);

            JsonNode currentNode = nodeMap.get(currentNodeId);
            if (currentNode == null) break;

            JsonNode data = currentNode.get("data");
            String nodeType = data.get("label").asText();
            String assignedRole = data.has("assignedRole") ? data.get("assignedRole").asText() : "";
            log.info("Processing: id={}, type={}, role={}", currentNodeId, nodeType, assignedRole);

            // XỬ LÝ APPROVAL NODE
            if ("APPROVAL".equals(nodeType)) {
                List<EdgeInfo> outgoingEdges = edgesMap.get(currentNodeId);
                String nextNodeId = null;
                List<String> possibleActions = new ArrayList<>();

                if (outgoingEdges != null && !outgoingEdges.isEmpty()) {
                    for (EdgeInfo ei : outgoingEdges) {
                        possibleActions.add(ei.getEdgeType());
                        if ("conditional".equals(ei.getEdgeType()) && nextNodeId == null) {
                            nextNodeId = ei.getTarget();
                        }
                    }
                    log.info("Outgoing edges: {}, nextNodeId: {}", possibleActions, nextNodeId);
                }

                String action = approvalActions != null ? approvalActions.get(currentNodeId) : null;
                ApprovalDetail detail = approvalDetails != null ? approvalDetails.get(currentNodeId) : null;

                WorkflowStep step = WorkflowStep.builder()
                        .stepOrder(stepOrder)
                        .stepName(assignedRole)
                        .assignedRole(assignedRole)
                        .approvalType("SINGLE")
                        .nodeId(currentNodeId)
                        .nextNodeIfConditional(nextNodeId)
                        .possibleActions(possibleActions)
                        .isEndStep(false)
                        .waitingForAction(action == null)
                        .approved(action != null && "APPROVED".equals(action))
                        .status(action == null ? StepStatus.PENDING :
                                ("APPROVED".equals(action) ? StepStatus.APPROVED : StepStatus.REJECTED))
                        .build();

                steps.add(step);
                log.info("Step {}: role={}, nodeId={}, waiting={}", stepOrder, assignedRole, currentNodeId, action == null);

                if (action == null) {
                    log.info("⏸️ Stopping at pending step: {}", assignedRole);
                    break;
                } else {
                    log.info("✅ Step {} completed, moving to next", stepOrder);
                    currentNodeId = nextNodeId;
                    stepOrder++;
                }
                continue;
            }

            // XỬ LÝ END NODE (có role) - LƯU Ý: lưu nextNodeId = null để biết là bước cuối
            if ("END".equals(nodeType) && assignedRole != null && !assignedRole.isEmpty()) {
                log.info("📍 Processing END node with role: {}", assignedRole);

                String action = approvalActions != null ? approvalActions.get(currentNodeId) : null;

                WorkflowStep step = WorkflowStep.builder()
                        .stepOrder(stepOrder)
                        .stepName(assignedRole)
                        .assignedRole(assignedRole)
                        .approvalType("SINGLE")
                        .nodeId(currentNodeId)
                        .nextNodeIfConditional(null)
                        .isEndStep(true)
                        .waitingForAction(action == null)
                        .approved(action != null && "APPROVED".equals(action))
                        .status(action == null ? StepStatus.PENDING :
                                ("APPROVED".equals(action) ? StepStatus.APPROVED : StepStatus.REJECTED))
                        .build();

                steps.add(step);
                log.info("Step {}: END node role={}, waiting={}", stepOrder, assignedRole, action == null);

                if (action == null) {
                    log.info("⏸️ Stopping at END node step: {}", assignedRole);
                } else {
                    log.info("🏁 END node approved, workflow completed");
                }
                break;
            }

            // XỬ LÝ START - chuyển đến node đầu tiên
            if ("START".equals(nodeType)) {
                List<EdgeInfo> outgoing = edgesMap.get(currentNodeId);
                if (outgoing != null && !outgoing.isEmpty()) {
                    String nextId = outgoing.get(0).getTarget();
                    log.info("START -> next: {}", nextId);
                    currentNodeId = nextId;
                    continue;
                }
            }

            break;
        }

        log.info("Total steps parsed: {}", steps.size());
        for (WorkflowStep s : steps) {
            log.info("  Step {}: role={}, nodeId={}, isEndStep={}", s.getStepOrder(), s.getStepName(), s.getNodeId(), s.isEndStep());
        }
        return steps;
    }
    private String findStartNodeId(JsonNode nodes) {
        for (JsonNode node : nodes) {
            JsonNode data = node.get("data");
            if (data != null && data.has("label")) {
                if ("START".equals(data.get("label").asText())) {
                    String nodeId = node.get("id").asText();
                    log.info("✅ Found START node: id={}", nodeId);
                    return nodeId;
                }
            }
        }
        log.error("❌ No START node found");
        return null;
    }

    private String convertRoleToCode(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return "UNKNOWN";
        }
        // Giữ nguyên tên role để tìm trong database users.position
        return roleName;
    }

    private String findNextNodeByEdgeType(List<EdgeInfo> edges, String edgeType) {
        return edges.stream()
                .filter(e -> edgeType.equals(e.getEdgeType()))
                .map(EdgeInfo::getTarget)
                .findFirst()
                .orElse(null);
    }

    // ==================== CONVERTERS ====================

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

    private ApprovalResponseDTO convertToResponseDTO(ApprovalRequest request, UUID currentUserId) {
        List<ApprovalAction> actions = actionRepository.findByRequestIdOrderByStepOrderAsc(request.getId());

        String requesterName = "";
        try {
            Users requester = userRepository.findById(request.getRequesterId()).orElse(null);
            if (requester != null) {
                requesterName = requester.getName();
            }
        } catch (Exception e) {
            log.warn("Cannot get requester name");
        }

        List<ApprovalActionDTO> actionDTOs = actions.stream()
                .map(action -> convertToActionDTO(action, currentUserId, request.getCompanyId()))
                .collect(Collectors.toList());

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

        UUID requesterId = jwtUtil.getUserIdFromToken(dto.getToken());
        log.info("Requester ID from token: {}", requesterId);

        if (!userRepository.existsById(requesterId)) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }

        Workflow workflow = getActiveWorkflow(companyId, dto.getRequestType());
        log.info("Found workflow: {} (id: {})", workflow.getName(), workflow.getWorkflowId());

        try {
            String nodesStr = cleanJsonString(workflow.getNodes());
            String edgesStr = cleanJsonString(workflow.getEdges());

            log.info("=== WORKFLOW DATA ===");
            log.info("Nodes: {}", nodesStr);
            log.info("Edges: {}", edgesStr);

            JsonNode nodes = objectMapper.readTree(nodesStr);
            JsonNode edges = objectMapper.readTree(edgesStr);

            Map<String, String> emptyActions = new HashMap<>();
            Map<String, ApprovalDetail> emptyDetails = new HashMap<>();

            List<WorkflowStep> steps = parseWorkflowSteps(nodes, edges, emptyActions, emptyDetails);

            log.info("=== STEPS PARSED ===");
            for (WorkflowStep step : steps) {
                log.info("Step {}: role={}, nodeId={}, waiting={}, isEndStep={}",
                        step.getStepOrder(), step.getStepName(), step.getNodeId(), step.isWaitingForAction(), step.isEndStep());
            }

            if (steps.isEmpty()) {
                throw new RuntimeException("Quy trình không có bước APPROVAL nào");
            }

            ApprovalRequest request = ApprovalRequest.builder()
                    .requestCode(generateRequestCode(companyId))
                    .companyId(companyId)
                    .requesterId(requesterId)
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .requestType(dto.getRequestType())
                    .status("PENDING")
                    .workflowStatus("IN_PROGRESS")
                    .currentStepOrder(1)
                    .workflowId(workflow.getWorkflowId())
                    .note(dto.getNote())
                    .build();

            request = requestRepository.save(request);
            log.info("✅ Approval request saved: id={}, code={}", request.getId(), request.getRequestCode());

            // Lấy step đầu tiên (stepOrder = 1)
            List<WorkflowStep> firstSteps = steps.stream()
                    .filter(s -> s.getStepOrder() == 1)
                    .collect(Collectors.toList());

            List<ApprovalAction> actions = new ArrayList<>();
            for (WorkflowStep step : firstSteps) {
                List<Users> approvers = getApproversByRole(companyId, step.getStepName());
                if (approvers.isEmpty()) {
                    log.warn("⚠️ No approver for role: {}", step.getStepName());
                    // Tạo action với approverId = null
                    ApprovalAction action = ApprovalAction.builder()
                            .requestId(request.getId())
                            .stepOrder(step.getStepOrder())
                            .stepName(step.getStepName())
                            .approvalType(step.getApprovalType())
                            .action("PENDING")
                            .actionStatus("PENDING")
                            .nodeId(step.getNodeId())
                            .possibleActions(String.join(",", step.getPossibleActions()))
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
                                .actionStatus("PENDING")
                                .nodeId(step.getNodeId())
                                .possibleActions(String.join(",", step.getPossibleActions()))
                                .build();
                        actions.add(action);
                        log.info("✅ Created action for step {}: approver={}", step.getStepOrder(), approver.getName());
                    }
                }
            }

            actionRepository.saveAll(actions);
            log.info("✅ Saved {} approval actions", actions.size());

            if (!firstSteps.isEmpty()) {
                request.setCurrentNodeId(firstSteps.get(0).getNodeId());
                requestRepository.save(request);
            }

            return convertToResponseDTO(request, requesterId);

        } catch (Exception e) {
            log.error("Error creating approval request: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi xử lý quy trình: " + e.getMessage());
        }
    }

    // ==================== PROCESS APPROVAL ====================
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

        ApprovalRequest request = requestRepository.findById(action.getRequestId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        Users currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Kiểm tra quyền
        List<Users> allowedApprovers = getApproversByRole(request.getCompanyId(), action.getStepName());
        boolean isAllowed = allowedApprovers.stream().anyMatch(u -> u.getUserId().equals(userId));

        if (!isAllowed) {
            throw new RuntimeException("Bạn không có quyền duyệt yêu cầu này");
        }

        Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy workflow"));

        // Cập nhật action
        action.setApproverId(userId);
        action.setApproverName(currentUser.getName());
        action.setAction(requestDTO.getAction());
        action.setApprovedAt(OffsetDateTime.now());
        action.setNote(requestDTO.getNote());
        action.setActionStatus("COMPLETED");

        if ("REJECTED".equals(requestDTO.getAction())) {
            action.setRejectionReason(requestDTO.getRejectionReason());
        } else if ("REQUEST_CHANGES".equals(requestDTO.getAction())) {
            action.setRejectionReason(requestDTO.getChangeRequestNote());
        }

        actionRepository.save(action);
        log.info("✅ Action {} updated with status: {}", action.getId(), requestDTO.getAction());

        // Xử lý theo action
        if ("REJECTED".equals(requestDTO.getAction())) {
            request.setStatus("REJECTED");
            request.setWorkflowStatus("REJECTED");
            request.setNote(requestDTO.getNote());
            request.setUpdatedAt(OffsetDateTime.now());
            requestRepository.save(request);
            cancelPendingActions(request.getId());

            return Map.of(
                    "message", "Yêu cầu đã bị từ chối",
                    "status", "REJECTED",
                    "requestId", request.getId()
            );
        } else if ("REQUEST_CHANGES".equals(requestDTO.getAction())) {
            // XỬ LÝ REQUEST_CHANGES: KHÔNG CHUYỂN TIẾP, chỉ cập nhật status
            request.setStatus("REQUEST_CHANGES");
            request.setWorkflowStatus("REQUEST_CHANGES");
            request.setUpdatedAt(OffsetDateTime.now());

            requestRepository.save(request);

            // Hủy tất cả các action đang PENDING khác
            cancelPendingActions(request.getId());

            return Map.of(
                    "message", "Đã yêu cầu chỉnh sửa. Người gửi sẽ cập nhật lại yêu cầu.",
                    "status", "REQUEST_CHANGES",
                    "requestId", request.getId(),
                    "changeRequestNote", action.getRejectionReason()
            );
        } else {
            // XỬ LÝ APPROVED - tìm bước tiếp theo
            return handleApprovalAndMoveToNext(action, request, workflow);
        }
    }

    /**
     * Xử lý khi duyệt và chuyển sang bước tiếp theo
     */
    private Map<String, Object> handleApprovalAndMoveToNext(ApprovalAction currentAction, ApprovalRequest request, Workflow workflow) {
        UUID requestId = currentAction.getRequestId();
        String currentNodeId = currentAction.getNodeId();
        int currentStepOrder = currentAction.getStepOrder();

        log.info("=== HANDLE APPROVAL AND MOVE TO NEXT ===");
        log.info("Current nodeId: {}, stepOrder: {}", currentNodeId, currentStepOrder);

        try {
            // Lấy và clean JSON string
            String edgesRaw = workflow.getEdges();
            String nodesRaw = workflow.getNodes();

            log.info("Raw edges: {}", edgesRaw);

            // CLEAN JSON STRING - QUAN TRỌNG
            String edgesStr = cleanJsonString(edgesRaw);
            String nodesStr = cleanJsonString(nodesRaw);

            log.info("Cleaned edges: {}", edgesStr);

            JsonNode nodes = objectMapper.readTree(nodesStr);
            JsonNode edges = objectMapper.readTree(edgesStr);

            log.info("Parsed - nodes count: {}, edges count: {}", nodes.size(), edges.size());

            // Log tất cả edges
            log.info("=== ALL EDGES FROM WORKFLOW ===");
            for (int i = 0; i < edges.size(); i++) {
                JsonNode edge = edges.get(i);
                String source = edge.get("source").asText();
                String target = edge.get("target").asText();
                String edgeType = "unknown";
                if (edge.has("data") && edge.get("data").has("type")) {
                    edgeType = edge.get("data").get("type").asText();
                }
                log.info("Edge {}: {} -> {} [type={}]", i, source, target, edgeType);
            }

            // Tìm node tiếp theo
            String nextNodeId = null;
            for (int i = 0; i < edges.size(); i++) {
                JsonNode edge = edges.get(i);
                String source = edge.get("source").asText();
                if (source.equals(currentNodeId)) {
                    nextNodeId = edge.get("target").asText();
                    log.info("Found matching edge: {} -> {}", source, nextNodeId);
                    break;
                }
            }

            log.info("Next node id: {}", nextNodeId);

            if (nextNodeId == null) {
                log.info("No next node found, workflow completed");
                request.setStatus("APPROVED");
                request.setWorkflowStatus("COMPLETED");
                request.setUpdatedAt(OffsetDateTime.now());
                requestRepository.save(request);

                return Map.of(
                        "message", "Yêu cầu đã được duyệt hoàn tất!",
                        "status", "APPROVED",
                        "requestId", requestId
                );
            }

            // Tìm node tiếp theo
            JsonNode nextNode = findNodeById(nodes, nextNodeId);
            if (nextNode == null) {
                log.error("Next node not found: {}", nextNodeId);
                throw new RuntimeException("Không tìm thấy node tiếp theo: " + nextNodeId);
            }

            JsonNode data = nextNode.get("data");
            String nodeType = data.get("label").asText();
            String assignedRole = data.has("assignedRole") ? data.get("assignedRole").asText() : "";

            log.info("Next node: id={}, type={}, role={}", nextNodeId, nodeType, assignedRole);

            // Xử lý END node
            if ("END".equals(nodeType)) {
                if (assignedRole != null && !assignedRole.isEmpty()) {
                    log.info("Creating action for END node role: {}", assignedRole);

                    List<Users> approvers = getApproversByRole(request.getCompanyId(), assignedRole);
                    int newStepOrder = currentStepOrder + 1;

                    if (approvers.isEmpty()) {
                        log.warn("⚠️ No approver found for role: {}", assignedRole);
                        request.setStatus("APPROVED");
                        request.setWorkflowStatus("COMPLETED");
                        request.setUpdatedAt(OffsetDateTime.now());
                        requestRepository.save(request);

                        return Map.of(
                                "message", "Yêu cầu đã được duyệt hoàn tất!",
                                "status", "APPROVED",
                                "requestId", requestId
                        );
                    }

                    List<ApprovalAction> newActions = new ArrayList<>();
                    for (Users approver : approvers) {
                        ApprovalAction newAction = ApprovalAction.builder()
                                .requestId(requestId)
                                .stepOrder(newStepOrder)
                                .stepName(assignedRole)
                                .approverId(approver.getUserId())
                                .approverName(approver.getName())
                                .approvalType("SINGLE")
                                .action("PENDING")
                                .actionStatus("PENDING")
                                .nodeId(nextNodeId)
                                .possibleActions("conditional")
                                .build();
                        newActions.add(newAction);
                        log.info("✅ Created action for END node: role={}, approver={}, stepOrder={}",
                                assignedRole, approver.getName(), newStepOrder);
                    }

                    actionRepository.saveAll(newActions);
                    log.info("✅ Saved {} actions for END node", newActions.size());

                    request.setCurrentNodeId(nextNodeId);
                    request.setCurrentStepOrder(newStepOrder);
                    request.setUpdatedAt(OffsetDateTime.now());
                    requestRepository.save(request);

                    return Map.of(
                            "message", "Đã duyệt thành công. Chuyển sang bước duyệt: " + assignedRole,
                            "status", "IN_PROGRESS",
                            "requestId", requestId,
                            "currentStep", currentStepOrder,
                            "nextStep", newStepOrder,
                            "nextStepName", assignedRole
                    );
                } else {
                    request.setStatus("APPROVED");
                    request.setWorkflowStatus("COMPLETED");
                    request.setUpdatedAt(OffsetDateTime.now());
                    requestRepository.save(request);

                    return Map.of(
                            "message", "Yêu cầu đã được duyệt hoàn tất!",
                            "status", "APPROVED",
                            "requestId", requestId
                    );
                }
            }

            // Xử lý APPROVAL node
            if ("APPROVAL".equals(nodeType)) {
                if (assignedRole == null || assignedRole.isEmpty()) {
                    throw new RuntimeException("APPROVAL node không có role được gán");
                }

                List<Users> approvers = getApproversByRole(request.getCompanyId(), assignedRole);
                int newStepOrder = currentStepOrder + 1;

                if (approvers.isEmpty()) {
                    log.warn("⚠️ No approver for role: {}", assignedRole);
                    ApprovalAction dummyAction = ApprovalAction.builder()
                            .requestId(requestId)
                            .stepOrder(newStepOrder)
                            .stepName(assignedRole)
                            .approvalType("SINGLE")
                            .action("SKIPPED")
                            .actionStatus("SKIPPED")
                            .nodeId(nextNodeId)
                            .build();
                    actionRepository.save(dummyAction);
                    return handleApprovalAndMoveToNext(dummyAction, request, workflow);
                }

                List<ApprovalAction> newActions = new ArrayList<>();
                for (Users approver : approvers) {
                    ApprovalAction newAction = ApprovalAction.builder()
                            .requestId(requestId)
                            .stepOrder(newStepOrder)
                            .stepName(assignedRole)
                            .approverId(approver.getUserId())
                            .approverName(approver.getName())
                            .approvalType("SINGLE")
                            .action("PENDING")
                            .actionStatus("PENDING")
                            .nodeId(nextNodeId)
                            .possibleActions("conditional")
                            .build();
                    newActions.add(newAction);
                    log.info("✅ Created action for role: {}, approver: {}, stepOrder: {}",
                            assignedRole, approver.getName(), newStepOrder);
                }

                actionRepository.saveAll(newActions);

                request.setCurrentNodeId(nextNodeId);
                request.setCurrentStepOrder(newStepOrder);
                request.setUpdatedAt(OffsetDateTime.now());
                requestRepository.save(request);

                return Map.of(
                        "message", "Đã duyệt thành công. Chuyển sang bước tiếp theo: " + assignedRole,
                        "status", "IN_PROGRESS",
                        "requestId", requestId,
                        "currentStep", currentStepOrder,
                        "nextStep", newStepOrder,
                        "nextStepName", assignedRole
                );
            }

            return Map.of(
                    "message", "Đã duyệt thành công",
                    "status", "IN_PROGRESS",
                    "requestId", requestId
            );

        } catch (Exception e) {
            log.error("Error in handleApprovalAndMoveToNext: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi xử lý duyệt: " + e.getMessage());
        }
    }

    private JsonNode findNodeById(JsonNode nodes, String nodeId) {
        for (JsonNode node : nodes) {
            if (node.get("id").asText().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }

    private void cancelPendingActions(UUID requestId) {
        List<ApprovalAction> pendingActions = actionRepository.findByRequestIdAndAction(requestId, "PENDING");
        for (ApprovalAction pendingAction : pendingActions) {
            pendingAction.setAction("CANCELLED");
            pendingAction.setActionStatus("CANCELLED");
            actionRepository.save(pendingAction);
        }
    }

    // ==================== QUERY METHODS ====================

    public List<ApprovalResponseDTO> getPendingRequestsForUser(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        log.info("Getting requests for user: {} (position: {})", user.getName(), user.getPosition());

        List<ApprovalRequest> requests = requestRepository.findRequestsByApproverRole(
                user.getCompanyId(),
                user.getPosition(),
                userId
        );

        log.info("Found {} requests", requests.size());

        return requests.stream()
                .map(request -> convertToResponseDTO(request, userId))
                .collect(Collectors.toList());
    }

    public ApprovalResponseDTO getRequestDetail(UUID requestId, UUID currentUserId) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));
        return convertToResponseDTO(request, currentUserId);
    }

    public List<ApprovalResponseDTO> getMyRequests(UUID requesterId) {
        List<ApprovalRequest> requests = requestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
        return requests.stream()
                .map(request -> convertToResponseDTO(request, requesterId))
                .collect(Collectors.toList());
    }

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

    public List<Workflow> getWorkflowsByCompany(UUID companyId) {
        return workflowRepository.findByCompanyIdAndStatusOrderByCreateAtDesc(companyId, "active");
    }

    public Workflow getActiveWorkflowByCompanyAndName(UUID companyId, String name) {
        return getActiveWorkflow(companyId, name);
    }
    public ApprovalRequest getRequestById(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));
    }

    /**
     * Cập nhật yêu cầu (khi bị yêu cầu chỉnh sửa)
     */
    @Transactional
    public ApprovalResponseDTO updateRequest(UUID requestId, UpdateApprovalRequestDTO updateDTO) {
        log.info("Updating request: id={}", requestId);

        // Tìm request
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        // Kiểm tra trạng thái
        if (!"REQUEST_CHANGES".equals(request.getStatus())) {
            throw new RuntimeException("Chỉ có thể chỉnh sửa yêu cầu khi đang ở trạng thái yêu cầu chỉnh sửa");
        }

        // Cập nhật thông tin
        if (updateDTO.getTitle() != null) {
            request.setTitle(updateDTO.getTitle());
        }
        if (updateDTO.getDescription() != null) {
            request.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getRequestType() != null) {
            request.setRequestType(updateDTO.getRequestType());
        }
        if (updateDTO.getNote() != null) {
            request.setNote(updateDTO.getNote());
        }

        // Chuyển trạng thái về PENDING để gửi lại duyệt
        request.setStatus("PENDING");
        request.setWorkflowStatus("IN_PROGRESS");
        request.setUpdatedAt(OffsetDateTime.now());

        requestRepository.save(request);
        log.info("✅ Request updated and status changed to PENDING: {}", requestId);

        // Lấy lại thông tin action hiện tại (bước đang chờ)
        // Cần tạo lại action cho bước đã bị REQUEST_CHANGES
        // Hoặc kích hoạt lại action cũ

        // Cách 1: Tìm action có action = "REQUEST_CHANGES" và chuyển thành "PENDING"
        List<ApprovalAction> actions = actionRepository.findByRequestIdOrderByStepOrderAsc(requestId);
        for (ApprovalAction action : actions) {
            if ("REQUEST_CHANGES".equals(action.getAction())) {
                action.setAction("PENDING");
                action.setActionStatus("PENDING");
                action.setRejectionReason(null); // Xóa lý do cũ
                actionRepository.save(action);
                log.info("✅ Reactivated action for step: {}", action.getStepOrder());
                break;
            }
        }

        // Cách 2: Nếu không tìm thấy action REQUEST_CHANGES, tìm action PENDING đầu tiên
        ApprovalAction pendingAction = actions.stream()
                .filter(a -> "PENDING".equals(a.getAction()))
                .findFirst()
                .orElse(null);

        if (pendingAction == null) {
            // Nếu không có action PENDING nào, tạo mới từ workflow
            Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy workflow"));

            try {
                JsonNode nodes = objectMapper.readTree(workflow.getNodes());
                JsonNode edges = objectMapper.readTree(workflow.getEdges());

                Map<String, String> emptyActions = new HashMap<>();
                Map<String, ApprovalDetail> emptyDetails = new HashMap<>();

                // Parse lại workflow để lấy steps
                List<WorkflowStep> steps = parseWorkflowSteps(nodes, edges, emptyActions, emptyDetails);

                // Tìm step đầu tiên (stepOrder = 1)
                WorkflowStep firstStep = steps.stream()
                        .filter(s -> s.getStepOrder() == 1)
                        .findFirst()
                        .orElse(null);

                if (firstStep != null) {
                    List<Users> approvers = getApproversByRole(request.getCompanyId(), firstStep.getAssignedRole());
                    for (Users approver : approvers) {
                        ApprovalAction newAction = ApprovalAction.builder()
                                .requestId(requestId)
                                .stepOrder(firstStep.getStepOrder())
                                .stepName(firstStep.getStepName())
                                .approverId(approver.getUserId())
                                .approverName(approver.getName())
                                .approvalType(firstStep.getApprovalType())
                                .action("PENDING")
                                .actionStatus("PENDING")
                                .nodeId(firstStep.getNodeId())
                                .possibleActions(String.join(",", firstStep.getPossibleActions()))
                                .build();
                        actionRepository.save(newAction);
                        log.info("✅ Created new action for step: {}", firstStep.getStepOrder());
                    }
                }
            } catch (Exception e) {
                log.error("Error recreating actions: {}", e.getMessage(), e);
                throw new RuntimeException("Lỗi tạo lại luồng duyệt: " + e.getMessage());
            }
        }

        // Chuyển về trang chi tiết
        return convertToResponseDTO(request, request.getRequesterId());
    }
}