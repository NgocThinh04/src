package com.example.project_.ELECTRONIC_OFFICE.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private Long totalEmployees;
    private Long totalRequests;
    private Long pendingRequests;
    private Long approvedRequests;
    private Long rejectedRequests;
    private List<RecentActivityDTO> recentActivities;
}
