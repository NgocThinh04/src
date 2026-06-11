package com.example.project_.ELECTRONIC_OFFICE.service.admin;

import com.example.project_.ELECTRONIC_OFFICE.dto.response.DashboardStatsDTO;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.RecentActivityDTO;
import com.example.project_.ELECTRONIC_OFFICE.entity.ApprovalAction;
import com.example.project_.ELECTRONIC_OFFICE.entity.ApprovalRequest;
import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import com.example.project_.ELECTRONIC_OFFICE.repository.admin.AdminRepository;
import com.example.project_.ELECTRONIC_OFFICE.repository.user.ApprovalActionRepository;
import com.example.project_.ELECTRONIC_OFFICE.repository.user.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AdminRepository userRepository;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalActionRepository actionRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Lấy thống kê dashboard theo companyId
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(UUID companyId) {
        log.info("Getting dashboard stats for company: {}", companyId);

        // Thống kê số lượng
        Long totalEmployees = userRepository.countByCompanyId(companyId);
        Long totalRequests = requestRepository.countByCompanyId(companyId);
        Long pendingRequests = requestRepository.countPendingByCompanyId(companyId);
        Long approvedRequests = requestRepository.countApprovedByCompanyId(companyId);
        Long rejectedRequests = requestRepository.countRejectedByCompanyId(companyId);

        // Lấy activity gần đây
        List<RecentActivityDTO> recentActivities = getRecentActivities(companyId);

        return DashboardStatsDTO.builder()
                .totalEmployees(totalEmployees != null ? totalEmployees : 0L)
                .totalRequests(totalRequests != null ? totalRequests : 0L)
                .pendingRequests(pendingRequests != null ? pendingRequests : 0L)
                .approvedRequests(approvedRequests != null ? approvedRequests : 0L)
                .rejectedRequests(rejectedRequests != null ? rejectedRequests : 0L)
                .recentActivities(recentActivities)
                .build();
    }

    /**
     * Lấy các hoạt động gần đây (yêu cầu mới, duyệt, từ chối)
     */
    private List<RecentActivityDTO> getRecentActivities(UUID companyId) {
        List<RecentActivityDTO> activities = new ArrayList<>();
        Pageable limit = PageRequest.of(0, 10);

        // 1. Lấy yêu cầu mới nhất
        List<ApprovalRequest> recentRequests = requestRepository.findTop10ByCompanyIdOrderByCreatedAtDesc(companyId, limit);
        for (ApprovalRequest request : recentRequests) {
            activities.add(RecentActivityDTO.builder()
                    .id(request.getId().toString())
                    .type("REQUEST_CREATED")
                    .title(request.getTitle())
                    .userName(getRequesterName(request.getRequesterId()))
                    .userRole("Người gửi")
                    .status(getStatusText(request.getStatus()))
                    .createdAt(request.getCreatedAt())
                    .formattedTime(formatTime(request.getCreatedAt()))
                    .build());
        }

        // 2. Lấy các action đã duyệt/từ chối gần đây
        List<ApprovalRequest> allRequests = requestRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        if (!allRequests.isEmpty()) {
            List<UUID> requestIds = allRequests.stream()
                    .map(ApprovalRequest::getId)
                    .collect(Collectors.toList());

            List<ApprovalAction> recentActions = actionRepository.findTop10ByRequestIdInOrderByApprovedAtDesc(requestIds, limit);
            for (ApprovalAction action : recentActions) {
                if (action.getApprovedAt() != null) {
                    String type = "APPROVED".equals(action.getAction()) ? "REQUEST_APPROVED" : "REQUEST_REJECTED";
                    activities.add(RecentActivityDTO.builder()
                            .id(action.getRequestId().toString())
                            .type(type)
                            .title(action.getStepName())
                            .userName(action.getApproverName())
                            .userRole(action.getStepName())
                            .status(type.equals("REQUEST_APPROVED") ? "Đã duyệt" : "Từ chối")
                            .createdAt(action.getApprovedAt())
                            .formattedTime(formatTime(action.getApprovedAt()))
                            .build());
                }
            }
        }

        // 3. Sắp xếp theo thời gian giảm dần và lấy 10 bản ghi đầu
        activities.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return activities.stream().limit(10).collect(Collectors.toList());
    }

    private String getRequesterName(UUID requesterId) {
        return userRepository.findById(requesterId)
                .map(Users::getName)
                .orElse("Không rõ");
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "Đang xử lý";
            case "APPROVED": return "Đã duyệt";
            case "REJECTED": return "Từ chối";
            case "REQUEST_CHANGES": return "Yêu cầu chỉnh sửa";
            default: return status;
        }
    }

    private String formatTime(OffsetDateTime time) {
        if (time == null) return "";
        return time.format(TIME_FORMATTER);
    }
}
