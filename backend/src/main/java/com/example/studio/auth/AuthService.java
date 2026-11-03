package com.example.studio.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.studio.common.ApiException;
import com.example.studio.common.ErrorCode;
import com.example.studio.common.Times;
import com.example.studio.config.AppProperties;

@Service
public class AuthService {

    public record Login(UserDto user, String token) {
    }

    private final UserMapper userMapper;
    private final SessionMapper sessionMapper;
    private final AppProperties props;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserMapper userMapper, SessionMapper sessionMapper, AppProperties props) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.props = props;
    }

    public Login register(String email, String password, String displayName) {
        String normalized = normalizeEmail(email);
        if (userMapper.findByEmail(normalized) != null) {
            throw new ApiException(ErrorCode.EMAIL_EXISTS, "该邮箱已注册");
        }
        String name = displayName == null ? "" : displayName.trim();
        if (name.isEmpty()) {
            name = normalized.substring(0, normalized.indexOf('@'));
            if (name.length() > 32) {
                name = name.substring(0, 32);
            }
        }
        User user = new User();
        user.email = normalized;
        user.passwordHash = encoder.encode(password);
        user.displayName = name;
        user.createdAt = Times.now();
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new ApiException(ErrorCode.EMAIL_EXISTS, "该邮箱已注册");
        }
        return new Login(UserDto.from(user), createSession(user.id));
    }

    public Login login(String email, String password) {
        User user = userMapper.findByEmail(normalizeEmail(email));
        if (user == null || !encoder.matches(password, user.passwordHash)) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "邮箱或密码错误");
        }
        return new Login(UserDto.from(user), createSession(user.id));
    }

    /** 返回 token 对应的用户 ID；token 无效或已过期返回 null（过期记录顺手删除）。 */
    public Long resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String hash = sha256(token);
        SessionMapper.Session session = sessionMapper.find(hash);
        if (session == null) {
            return null;
        }
        if (!session.expiresAt.isAfter(Instant.now())) {
            sessionMapper.delete(hash);
            return null;
        }
        return session.userId;
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessionMapper.delete(sha256(token));
        }
    }

    public UserDto me(long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return UserDto.from(user);
    }

    private String createSession(long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Times.now();
        sessionMapper.insert(sha256(token), userId, now, now.plus(props.auth().sessionTtl()));
        return token;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
