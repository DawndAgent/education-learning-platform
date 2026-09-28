package com.xxedu.learning.security;

import com.xxedu.learning.common.enums.ClientType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public final class JwtTokenService implements AccessTokenParser {

    static final String CLAIM_USER_ID = "uid";
    static final String CLAIM_CLIENT_TYPE = "clientType";
    static final String CLAIM_PERMISSIONS = "permissions";
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String issue(LoginUser user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getTtl().toMillis());
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_USER_ID, user.getUserId())
                .claim(CLAIM_CLIENT_TYPE, user.getClientType().name())
                .claim(CLAIM_PERMISSIONS, user.getPermissions())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public Optional<LoginUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number userId = claims.get(CLAIM_USER_ID, Number.class);
            String clientType = claims.get(CLAIM_CLIENT_TYPE, String.class);
            if (userId == null || claims.getSubject() == null || clientType == null) {
                return Optional.empty();
            }
            return Optional.of(new LoginUser(
                    userId.longValue(),
                    claims.getSubject(),
                    ClientType.valueOf(clientType),
                    readPermissions(claims.get(CLAIM_PERMISSIONS))));
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("jwt rejected");
            return Optional.empty();
        }
    }

    private Set<String> readPermissions(Object raw) {
        if (!(raw instanceof Collection<?> collection)) {
            return Set.of();
        }
        Set<String> permissions = new LinkedHashSet<>();
        for (Object item : collection) {
            if (item != null) {
                permissions.add(item.toString());
            }
        }
        return permissions;
    }
}
