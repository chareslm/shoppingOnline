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
        return Jwts.builder()
                .subject(String.valueOf(loginUser.userId()))
                .claim("username", loginUser.username())
                .claim("roles", loginUser.roles())
                .claim("permissions", loginUser.permissions())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(signingKey)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public LoginUser parseAccessToken(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        List<String> roles = claims.get("roles", List.class);
        List<String> permissions = claims.get("permissions", List.class);
        return new LoginUser(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                roles == null ? Set.of() : Set.copyOf(roles),
                permissions == null ? Set.of() : Set.copyOf(permissions)
        );
    }
}
