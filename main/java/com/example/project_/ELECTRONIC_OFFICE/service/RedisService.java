package com.example.project_.ELECTRONIC_OFFICE.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Lưu refresh token
    public void saveRefreshToken(String username, String refreshToken, long expirationMillis) {
        String key = "refresh_token:" + username;
        redisTemplate.opsForValue().set(key, refreshToken, expirationMillis, TimeUnit.MILLISECONDS);
        log.info("Saved refresh token for user: {}", username);
    }

    // Lấy refresh token
    public String getRefreshToken(String username) {
        String key = "refresh_token:" + username;
        Object token = redisTemplate.opsForValue().get(key);
        return token != null ? token.toString() : null;
    }

    // Xóa refresh token
    public void deleteRefreshToken(String username) {
        String key = "refresh_token:" + username;
        redisTemplate.delete(key);
        log.info("Deleted refresh token for user: {}", username);
    }

    // Kiểm tra refresh token có tồn tại không
    public boolean hasRefreshToken(String username) {
        String key = "refresh_token:" + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}