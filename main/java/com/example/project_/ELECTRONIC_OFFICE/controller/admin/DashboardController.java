package com.example.project_.ELECTRONIC_OFFICE.controller.admin;

import com.example.project_.ELECTRONIC_OFFICE.dto.response.DashboardStatsDTO;
import com.example.project_.ELECTRONIC_OFFICE.service.admin.DashboardService;
import com.example.project_.ELECTRONIC_OFFICE.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final JwtUtil jwtUtil;

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(@RequestHeader("Authorization") String authHeader) {
        log.info("GET /api/admin/dashboard/stats");

        try {
            UUID userId = jwtUtil.getUserIdFromToken(authHeader);
            UUID companyId = jwtUtil.getCompanyIdFromToken(authHeader);

            DashboardStatsDTO stats = dashboardService.getDashboardStats(companyId);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error getting dashboard stats: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}