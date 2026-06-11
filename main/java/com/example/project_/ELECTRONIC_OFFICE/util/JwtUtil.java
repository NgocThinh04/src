package com.example.project_.ELECTRONIC_OFFICE.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Giải mã token và lấy userId
     */
    public UUID getUserIdFromToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Lấy userId từ claims (không phải từ subject)
            String userId = claims.get("userId", String.class);

            if (userId == null) {
                throw new RuntimeException("User ID not found in token");
            }

            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid UUID format: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Invalid token: " + e.getMessage());
        }
    }
    public UUID getCompanyIdFromToken(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String companyId = claims.get("companyId", String.class);

            if (companyId == null) {
                throw new RuntimeException("Company ID not found in token");
            }

            return UUID.fromString(companyId);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid UUID format: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Invalid token: " + e.getMessage());
        }
    }
    /**
     * Giải mã token và lấy toàn bộ claims
     */
    public Claims getClaimsFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}