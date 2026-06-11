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
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.contains("\\\"")) {
            cleaned = cleaned.replace("\\\"", "\"");
        }
        if (cleaned.contains("\\\\")) {
            cleaned = cleaned.replace("\\\\", "\\");
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

        Map<String, List<EdgeInfo>> outgoingEdgesMap = new HashMap<>();
        Map<String, List<EdgeInfo>> incomingEdgesMap = new HashMap<>();

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

            outgoingEdgesMap.computeIfAbsent(source, k -> new ArrayList<>()).add(edgeInfo);
            incomingEdgesMap.computeIfAbsent(target, k -> new ArrayList<>()).add(edgeInfo);
        }

        log.info("=== ALL EDGES ===");
        for (Map.Entry<String, List<EdgeInfo>> entry : outgoingEdgesMap.entrySet()) {
            for (EdgeInfo ei : entry.getValue()) {
                log.info("Edge: {} -> {} [type={}]", entry.getKey(), ei.getTarget(), ei.getEdgeType());
            }
        }

        String startNodeId = findStartNodeId(nodes);
        if (startNodeId == null) {
            throw new RuntimeException("Không tìm thấy node START");
        }
        log.info("START node: {}", startNodeId);

        // BFS để duyệt workflow và tính toán stepOrder
        Map<String, Integer> nodeToStepOrder = new HashMap<>();
        Map<String, String> nodeToNextNode = new HashMap<>();
        Map<Integer, List<WorkflowStep>> stepsByOrder = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(startNodeId);
        nodeToStepOrder.put(startNodeId, 0);

        while (!queue.isEmpty()) {
            String currentNodeId = queue.poll();
            int currentStepOrder = nodeToStepOrder.get(currentNodeId);
            visited.add(currentNodeId);

            JsonNode currentNode = nodeMap.get(currentNodeId);
            if (currentNode == null) continue;

            JsonNode data = currentNode.get("data");
            String nodeType = data.get("label").asText();
            String assignedRole = data.has("assignedRole") ? data.get("assignedRole").asText() : "";

            // Nếu là APPROVAL node hoặc END node có role
            if ("APPROVAL".equals(nodeType) || ("END".equals(nodeType) && assignedRole != null && !assignedRole.isEmpty())) {
                boolean isEndNode = "END".equals(nodeType);

                // Lấy possibleActions từ incoming edges
                List<String> possibleActions = new ArrayList<>();
                List<EdgeInfo> incomingEdges = incomingEdgesMap.get(currentNodeId);
                if (incomingEdges != null && !incomingEdges.isEmpty()) {
                    for (EdgeInfo ei : incomingEdges) {
                        String edgeType = ei.getEdgeType();
                        if ("conditional".equals(edgeType)) {
                            possibleActions.add("Đồng ý");
                        } else if ("parallel".equals(edgeType)) {
                            possibleActions.add("Đồng ý (Song song)");
                        } else if ("reject".equals(edgeType)) {
                            possibleActions.add("Từ chối");
                        }
                    }
                }
                if (possibleActions.isEmpty()) {
                    possibleActions.add("Đồng ý");
                    possibleActions.add("Từ chối");
                }

                // Lấy node tiếp theo
                String nextNodeId = null;
                List<EdgeInfo> outgoingEdges = outgoingEdgesMap.get(currentNodeId);
                if (outgoingEdges != null && !outgoingEdges.isEmpty()) {
                    nextNodeId = outgoingEdges.get(0).getTarget();
                    nodeToNextNode.put(currentNodeId, nextNodeId);
                }

                WorkflowStep step = WorkflowStep.builder()
                        .stepOrder(currentStepOrder)
                        .stepName(assignedRole)
                        .assignedRole(assignedRole)
                        .approvalType("SINGLE")
                        .nodeId(currentNodeId)
                        .nextNodeIfConditional(nextNodeId)
                        .possibleActions(possibleActions)
                        .isEndStep(isEndNode)
                        .waitingForAction(true)
                        .status(StepStatus.PENDING)
                        .build();

                stepsByOrder.computeIfAbsent(currentStepOrder, k -> new ArrayList<>()).add(step);
                log.info("Step {}: role={}, nodeId={}, isEndStep={}, possibleActions={}",
                        currentStepOrder, assignedRole, currentNodeId, isEndNode, possibleActions);
            }

            // Thêm các node con vào queue
            List<EdgeInfo> outgoingEdges = outgoingEdgesMap.get(currentNodeId);
            if (outgoingEdges != null) {
                for (EdgeInfo ei : outgoingEdges) {
                    String targetId = ei.getTarget();
                    if (!visited.contains(targetId) && !nodeToStepOrder.containsKey(targetId)) {
                        nodeToStepOrder.put(targetId, currentStepOrder + 1);
                        queue.add(targetId);
                    }
                }
            }
        }

        // Gom các steps theo đúng thứ tự stepOrder (từ 1 đến max)
        for (int i = 1; i <= stepsByOrder.size(); i++) {
            if (stepsByOrder.containsKey(i)) {
                steps.addAll(stepsByOrder.get(i));
            }
        }

        log.info("Total steps parsed: {}", steps.size());
        for (WorkflowStep s : steps) {
            log.info("  Step {}: role={}, nodeId={}, isEndStep={}, possibleActions={}",
                    s.getStepOrder(), s.getStepName(), s.getNodeId(), s.isEndStep(), s.getPossibleActions());
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
        return roleName;
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

            JsonNode nodes = objectMapper.readTree(nodesStr);
            JsonNode edges = objectMapper.readTree(edgesStr);

            Map<String, String> emptyActions = new HashMap<>();
            Map<String, ApprovalDetail> emptyDetails = new HashMap<>();

            List<WorkflowStep> steps = parseWorkflowSteps(nodes, edges, emptyActions, emptyDetails);

            log.info("=== STEPS PARSED ===");
            for (WorkflowStep step : steps) {
                log.info("Step {}: role={}, nodeId={}, isEndStep={}",
                        step.getStepOrder(), step.getStepName(), step.getNodeId(), step.isEndStep());
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

            // CHỈ tạo action cho step 1 (các node đầu tiên)
            List<WorkflowStep> step1Steps = steps.stream()
                    .filter(s -> s.getStepOrder() == 1)
                    .collect(Collectors.toList());

            List<ApprovalAction> actions = new ArrayList<>();
            for (WorkflowStep step : step1Steps) {
                List<Users> approvers = getApproversByRole(companyId, step.getStepName());
                String possibleActionsStr = step.getPossibleActions() != null
                        ? String.join(",", step.getPossibleActions())
                        : "Đồng ý,Từ chối";

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
                            .possibleActions(possibleActionsStr)
                            .build();
                    actions.add(action);
                    log.info("✅ Created action for step {}: role={}, approver={}",
                            step.getStepOrder(), step.getStepName(), approver.getName());
                }
            }

            actionRepository.saveAll(actions);
            log.info("✅ Saved {} approval actions for step 1", actions.size());

            if (!step1Steps.isEmpty()) {
                request.setCurrentNodeId(step1Steps.get(0).getNodeId());
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

        List<Users> allowedApprovers = getApproversByRole(request.getCompanyId(), action.getStepName());
        boolean isAllowed = allowedApprovers.stream().anyMatch(u -> u.getUserId().equals(userId));

        if (!isAllowed) {
            throw new RuntimeException("Bạn không có quyền duyệt yêu cầu này");
        }

        Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy workflow"));

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
            request.setStatus("REQUEST_CHANGES");
            request.setWorkflowStatus("REQUEST_CHANGES");
            request.setUpdatedAt(OffsetDateTime.now());
            requestRepository.save(request);
            cancelPendingActions(request.getId());

            return Map.of(
                    "message", "Đã yêu cầu chỉnh sửa. Người gửi sẽ cập nhật lại yêu cầu.",
                    "status", "REQUEST_CHANGES",
                    "requestId", request.getId(),
                    "changeRequestNote", action.getRejectionReason()
            );
        } else {
            return handleApprovalAndMoveToNext(action, request, workflow);
        }
    }

    /**
     * Xử lý khi duyệt và chuyển sang bước tiếp theo
     * QUAN TRỌNG: Loại kết nối được xác định từ INCOMING edge (từ node cha)
     */
    private Map<String, Object> handleApprovalAndMoveToNext(ApprovalAction currentAction, ApprovalRequest request, Workflow workflow) {
        UUID requestId = currentAction.getRequestId();
        String currentNodeId = currentAction.getNodeId();
        int currentStepOrder = currentAction.getStepOrder();

        log.info("=== HANDLE APPROVAL AND MOVE TO NEXT ===");
        log.info("Current nodeId: {}, stepOrder: {}", currentNodeId, currentStepOrder);

        try {
            String edgesRaw = workflow.getEdges();
            String nodesRaw = workflow.getNodes();

            String edgesStr = cleanJsonString(edgesRaw);
            String nodesStr = cleanJsonString(nodesRaw);

            JsonNode nodes = objectMapper.readTree(nodesStr);
            JsonNode edges = objectMapper.readTree(edgesStr);

            log.info("Parsed - nodes count: {}, edges count: {}", nodes.size(), edges.size());

            // === QUAN TRỌNG: LẤY LOẠI KẾT NỐI TỪ INCOMING EDGE (từ node cha) ===
            String incomingConnectionType = getIncomingConnectionType(currentNodeId, edges);
            log.info("Incoming connection type to node {}: {}", currentNodeId, incomingConnectionType);

            // === LẤY TẤT CẢ ACTIONS CÙNG STEP ORDER ===
            List<ApprovalAction> sameStepActions = actionRepository.findByRequestIdAndStepOrder(requestId, currentStepOrder);
            log.info("Same step actions count: {}", sameStepActions.size());

            for (ApprovalAction a : sameStepActions) {
                log.info("  Action: stepOrder={}, stepName={}, action={}, approverName={}",
                        a.getStepOrder(), a.getStepName(), a.getAction(), a.getApproverName());
            }

            long totalCount = sameStepActions.size();
            long approvedCount = sameStepActions.stream()
                    .filter(a -> "APPROVED".equals(a.getAction()))
                    .count();

            List<String> approvedNames = sameStepActions.stream()
                    .filter(a -> "APPROVED".equals(a.getAction()))
                    .map(ApprovalAction::getApproverName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<String> pendingNames = sameStepActions.stream()
                    .filter(a -> "PENDING".equals(a.getAction()))
                    .map(ApprovalAction::getStepName)
                    .collect(Collectors.toList());

            boolean isStepCompleted = false;

            // Dựa vào incoming connection type để quyết định
            if ("parallel".equals(incomingConnectionType)) {
                // Parallel: chỉ cần 1 người duyệt là đủ
                isStepCompleted = approvedCount >= 1;
                log.info("Parallel mode (incoming) - Approved: {}/{}, Completed: {}", approvedCount, totalCount, isStepCompleted);

                if (!isStepCompleted) {
                    return Map.of(
                            "message", "Đã ghi nhận duyệt. Chỉ cần 1 người duyệt là đủ để chuyển tiếp!",
                            "status", "PROCESSING",
                            "requestId", requestId,
                            "currentStep", currentStepOrder,
                            "approvedCount", approvedCount,
                            "totalCount", totalCount,
                            "approvedNames", approvedNames
                    );
                }
            } else {
                // Conditional: cần TẤT CẢ duyệt
                isStepCompleted = totalCount > 0 && approvedCount == totalCount;
                log.info("Conditional mode (incoming) - Approved: {}/{}, Completed: {}", approvedCount, totalCount, isStepCompleted);

                if (!isStepCompleted) {
                    String message = "Đã ghi nhận duyệt của " + String.join(", ", approvedNames) +
                            ". Chờ thêm " + String.join(", ", pendingNames) + " duyệt.";
                    log.info(message);
                    return Map.of(
                            "message", message,
                            "status", "PROCESSING",
                            "requestId", requestId,
                            "currentStep", currentStepOrder,
                            "approvedCount", approvedCount,
                            "totalCount", totalCount,
                            "approvedNames", approvedNames,
                            "pendingNames", pendingNames
                    );
                }
            }

            // === TÌM NODE TIẾP THEO ===
            String nextNodeId = null;
            for (JsonNode edge : edges) {
                String source = edge.get("source").asText();
                if (source.equals(currentNodeId)) {
                    nextNodeId = edge.get("target").asText();
                    log.info("Found outgoing edge: {} -> {}", source, nextNodeId);
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
                        "requestId", requestId,
                        "approvedByAll", approvedNames
                );
            }

            JsonNode nextNode = findNodeById(nodes, nextNodeId);
            if (nextNode == null) {
                log.error("Next node not found: {}", nextNodeId);
                throw new RuntimeException("Không tìm thấy node tiếp theo: " + nextNodeId);
            }

            JsonNode data = nextNode.get("data");
            String nodeType = data.get("label").asText();
            String assignedRole = data.has("assignedRole") ? data.get("assignedRole").asText() : "";
            boolean isEndNode = "END".equals(nodeType);

            log.info("Next node: id={}, type={}, role={}", nextNodeId, nodeType, assignedRole);

            // Nếu là END node và không có role thì kết thúc
            if (isEndNode && (assignedRole == null || assignedRole.isEmpty())) {
                request.setStatus("APPROVED");
                request.setWorkflowStatus("COMPLETED");
                request.setUpdatedAt(OffsetDateTime.now());
                requestRepository.save(request);

                return Map.of(
                        "message", "Yêu cầu đã được duyệt hoàn tất!",
                        "status", "APPROVED",
                        "requestId", requestId,
                        "approvedByAll", approvedNames
                );
            }

            // Xác định possibleActions cho node tiếp theo (dựa trên incoming edge của node đó)
            List<String> nextPossibleActions = getPossibleActionsForNodeByIncoming(nextNodeId, edges);
            String possibleActionsStr = String.join(",", nextPossibleActions);
            log.info("Next node possible actions: {}", possibleActionsStr);

            int newStepOrder = currentStepOrder + 1;

            // Tạo action cho node tiếp theo
            List<Users> approvers = getApproversByRole(request.getCompanyId(), assignedRole);
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
                        .possibleActions(possibleActionsStr)
                        .build();
                newActions.add(newAction);
                log.info("✅ Created action for node: role={}, approver={}, stepOrder={}",
                        assignedRole, approver.getName(), newStepOrder);
            }

            actionRepository.saveAll(newActions);
            log.info("✅ Saved {} actions for step {}", newActions.size(), newStepOrder);

            request.setCurrentNodeId(nextNodeId);
            request.setCurrentStepOrder(newStepOrder);
            request.setUpdatedAt(OffsetDateTime.now());
            requestRepository.save(request);

            return Map.of(
                    "message", isEndNode ? "Đã duyệt thành công. Hoàn thành quy trình!" : "Đã duyệt thành công. Chuyển sang bước tiếp theo: " + assignedRole,
                    "status", isEndNode ? "APPROVED" : "IN_PROGRESS",
                    "requestId", requestId,
                    "currentStep", currentStepOrder,
                    "nextStep", newStepOrder,
                    "nextStepName", assignedRole,
                    "approvedByAll", approvedNames
            );

        } catch (Exception e) {
            log.error("Error in handleApprovalAndMoveToNext: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi xử lý duyệt: " + e.getMessage());
        }
    }

    /**
     * Lấy loại kết nối từ edge đi VÀO node (từ node cha)
     */
    private String getIncomingConnectionType(String nodeId, JsonNode edges) {
        for (JsonNode edge : edges) {
            String target = edge.get("target").asText();
            if (target.equals(nodeId)) {
                if (edge.has("data") && edge.get("data").has("type")) {
                    String edgeType = edge.get("data").get("type").asText();
                    log.info("Incoming edge to {} has type: {}", nodeId, edgeType);
                    return edgeType;
                }
            }
        }
        log.info("No incoming edge found for node {}, defaulting to conditional", nodeId);
        return "conditional";
    }

    /**
     * Lấy possible actions cho node dựa trên edge đi VÀO (từ node cha)
     */
    private List<String> getPossibleActionsForNodeByIncoming(String nodeId, JsonNode edges) {
        List<String> actions = new ArrayList<>();

        for (JsonNode edge : edges) {
            String target = edge.get("target").asText();
            if (target.equals(nodeId)) {
                if (edge.has("data") && edge.get("data").has("type")) {
                    String edgeType = edge.get("data").get("type").asText();
                    if ("conditional".equals(edgeType)) {
                        actions.add("Đồng ý");
                    } else if ("parallel".equals(edgeType)) {
                        actions.add("Đồng ý (Song song)");
                    } else if ("reject".equals(edgeType)) {
                        actions.add("Từ chối");
                    }
                }
            }
        }

        if (actions.isEmpty()) {
            actions.add("Đồng ý");
            actions.add("Từ chối");
        }

        log.info("Possible actions for node {} (by incoming): {}", nodeId, actions);
        return actions;
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

        List<ApprovalRequest> requests = requestRepository.findRequestsByApproverRole(
                user.getCompanyId(),
                user.getPosition(),
                userId
        );

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

    @Transactional
    public ApprovalResponseDTO updateRequest(UUID requestId, UpdateApprovalRequestDTO updateDTO) {
        log.info("Updating request: id={}", requestId);

        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (!"REQUEST_CHANGES".equals(request.getStatus())) {
            throw new RuntimeException("Chỉ có thể chỉnh sửa yêu cầu khi đang ở trạng thái yêu cầu chỉnh sửa");
        }

        if (updateDTO.getTitle() != null) request.setTitle(updateDTO.getTitle());
        if (updateDTO.getDescription() != null) request.setDescription(updateDTO.getDescription());
        if (updateDTO.getRequestType() != null) request.setRequestType(updateDTO.getRequestType());
        if (updateDTO.getNote() != null) request.setNote(updateDTO.getNote());

        request.setStatus("PENDING");
        request.setWorkflowStatus("IN_PROGRESS");
        request.setUpdatedAt(OffsetDateTime.now());
        requestRepository.save(request);

        List<ApprovalAction> actions = actionRepository.findByRequestIdOrderByStepOrderAsc(requestId);
        for (ApprovalAction action : actions) {
            if ("REQUEST_CHANGES".equals(action.getAction())) {
                action.setAction("PENDING");
                action.setActionStatus("PENDING");
                action.setRejectionReason(null);
                actionRepository.save(action);
                log.info("✅ Reactivated action for step: {}", action.getStepOrder());
                break;
            }
        }

        return convertToResponseDTO(request, request.getRequesterId());
    }
}