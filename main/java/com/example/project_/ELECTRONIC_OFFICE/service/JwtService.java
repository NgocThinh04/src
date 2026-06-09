// service/JwtService.java
package com.example.project_.ELECTRONIC_OFFICE.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.access-expiration}")
    private Long ACCESS_EXPIRATION_TIME;

    @Value("${jwt.refresh-expiration}")
    Long REFRESH_EXPIRATION_TIME;

    private Key getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // THÊM: Lấy userId từ token
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    // THÊM: Lấy companyId từ token
    public String extractCompanyId(String token) {
        return extractClaim(token, claims -> claims.get("companyId", String.class));
    }

    // THÊM: Lấy companyCode từ token
    public String extractCompanyCode(String token) {
        return extractClaim(token, claims -> claims.get("companyCode", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // SỬA: Thêm userId và companyId vào Access Token
    public String generateAccessToken(UserDetails userDetails, String userId, String companyId, String companyCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("roles", userDetails.getAuthorities());
        claims.put("userId", userId);
        claims.put("companyId", companyId);
        claims.put("companyCode", companyCode);
        return createToken(claims, userDetails.getUsername(), ACCESS_EXPIRATION_TIME);
    }

    // SỬA: Thêm userId và companyId vào Refresh Token
    public String generateRefreshToken(UserDetails userDetails, String userId, String companyId, String companyCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("userId", userId);
        claims.put("companyId", companyId);
        claims.put("companyCode", companyCode);
        return createToken(claims, userDetails.getUsername(), REFRESH_EXPIRATION_TIME);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Boolean validateRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "refresh".equals(claims.get("type")) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}