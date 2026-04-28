package com.example.auth.service;

import com.example.auth.domain.User;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.dto.TokenResponse;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public TokenResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        if ("ADMIN".equalsIgnoreCase(req.getRole())) {
            user.setRole(User.Role.ADMIN);
        }
        userRepository.save(user);
        return issueTokens(user);
    }

    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return issueTokens(user);
    }

    public void logout(String accessToken, String refreshToken) {
        try {
            Claims claims = jwtUtil.parse(accessToken);
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remaining > 0) {
                redisTemplate.opsForValue().set("blacklist:" + accessToken, "1", Duration.ofMillis(remaining));
            }
        } catch (Exception ignored) {}

        if (refreshToken != null) {
            redisTemplate.delete("refresh:" + refreshToken);
        }
    }

    public TokenResponse refresh(String refreshToken) {
        String email = redisTemplate.opsForValue().get("refresh:" + refreshToken);
        if (email == null) throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        redisTemplate.delete("refresh:" + refreshToken);
        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        String access = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refresh = jwtUtil.generateRefreshToken(user.getEmail());
        redisTemplate.opsForValue().set(
                "refresh:" + refresh, user.getEmail(),
                Duration.ofMillis(jwtUtil.getRefreshExpiration()));
        return new TokenResponse(access, refresh, user.getRole().name(), user.getId(), user.getEmail());
    }
}
