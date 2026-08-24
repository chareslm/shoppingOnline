package com.chareslm.shopping.security.jwt;

import com.chareslm.shopping.security.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtTokenService {
    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(LoginUser loginUser) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(loginUser.userId()))
                .claim("username", loginUser.username())
                .claim("roles", loginUser.roles())
                .claim("permissions", loginUser.permissions())
                // Signed claim lets the request filter enforce first-login password change without a client hint.
                .claim("mustChangePassword", loginUser.mustChangePassword())
                .claim("tokenType", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())));
        if (loginUser.deviceId() != null) {
            builder.claim("deviceId", loginUser.deviceId());
        }
        return builder.signWith(signingKey).compact();
    }

    @SuppressWarnings("unchecked")
    public LoginUser parseAccessToken(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        if (!"access".equals(claims.get("tokenType", String.class))) {
            throw new IllegalArgumentException("Not an access token");
        }
        List<String> roles = claims.get("roles", List.class);
        List<String> permissions = claims.get("permissions", List.class);
        Number deviceId = claims.get("deviceId", Number.class);
        return new LoginUser(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                roles == null ? Set.of() : Set.copyOf(roles),
                permissions == null ? Set.of() : Set.copyOf(permissions),
                Boolean.TRUE.equals(claims.get("mustChangePassword", Boolean.class))
        );
    }

    public IssuedRefreshToken createRefreshToken(Long userId, Long deviceId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.refreshTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .id(tokenId)
                .claim("tokenType", "refresh")
                .claim("deviceId", deviceId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedRefreshToken(token, tokenId, expiresAt);
    }

    public RefreshTokenClaims parseRefreshToken(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        if (!"refresh".equals(claims.get("tokenType", String.class)) || claims.getId() == null) {
            throw new IllegalArgumentException("Not a refresh token");
        }
        Number deviceId = claims.get("deviceId", Number.class);
        if (deviceId == null) {
            throw new IllegalArgumentException("Refresh token does not contain a device id");
        }
        return new RefreshTokenClaims(Long.valueOf(claims.getSubject()), deviceId.longValue(), claims.getId());
    }

    public record IssuedRefreshToken(String token, String tokenId, Instant expiresAt) {
    }

    public record RefreshTokenClaims(Long userId, Long deviceId, String tokenId) {
    }
}
