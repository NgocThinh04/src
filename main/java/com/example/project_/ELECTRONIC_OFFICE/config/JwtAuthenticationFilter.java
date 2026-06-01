// config/JwtAuthenticationFilter.java
package com.example.project_.ELECTRONIC_OFFICE.config;

import com.example.project_.ELECTRONIC_OFFICE.service.CustomUserDetailsService;
import com.example.project_.ELECTRONIC_OFFICE.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Bỏ qua filter cho các endpoint auth - KHÔNG CẦN TOKEN
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Bỏ qua tất cả các endpoint bắt đầu bằng /api/auth/
        boolean shouldSkip = path.startsWith("/api/auth/");
        if (shouldSkip) {
            log.info("🔓 Skipping JWT filter for: {}", path);
        }
        return shouldSkip;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String path = request.getRequestURI();

        log.info("🔍 Processing request: {}", path);

        // Kiểm tra header Authorization
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("⚠️ Missing or invalid Authorization header for: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            final String username = jwtService.extractUsername(token);
            log.info("📌 Extracted username from token: {}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("✅ Successfully authenticated user: {}", username);
                } else {
                    log.warn("⚠️ Invalid token for user: {}", username);
                }
            }
        } catch (ExpiredJwtException e) {
            log.error("❌ Token expired: {}", e.getMessage());
            // Không throw exception, chỉ log và tiếp tục filter chain
            // Không set response status để tránh ảnh hưởng đến các request khác
        } catch (Exception e) {
            log.error("❌ JWT validation error: {}", e.getMessage());
            // Không throw exception, chỉ log
        }

        filterChain.doFilter(request, response);
    }
}