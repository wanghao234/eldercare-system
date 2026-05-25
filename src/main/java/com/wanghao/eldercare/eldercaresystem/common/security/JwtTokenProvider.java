package com.wanghao.eldercare.eldercaresystem.common.security;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {
    private static final String DEFAULT_SECRET = "change-me-change-me-change-me-change-me";

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        String configuredSecret = resolveSecret();
        this.secretKey = Keys.hmacShaKeyFor(configuredSecret.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveSecret() {
        if (StringUtils.hasText(jwtProperties.getSecret())) {
            return jwtProperties.getSecret().trim();
        }
        return DEFAULT_SECRET;
    }

    public String createToken(Long userId, String username, String role, String status, Set<String> perms) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getExpirationSeconds());

        var builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .addClaims(Map.of(
                        "username", username,
                        "role", role,
                        "status", status,
                        "perms", new ArrayList<>(perms == null ? Set.of() : perms)
                ))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(secretKey, SignatureAlgorithm.HS256);

        if (jwtProperties.getIssuer() != null && !jwtProperties.getIssuer().isBlank()) {
            builder.setIssuer(jwtProperties.getIssuer());
        }

        return builder.compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String getStatus(String token) {
        return parseClaims(token).get("status", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPerms(String token) {
        Object raw = parseClaims(token).get("perms");
        if (raw instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item instanceof String value && !value.isBlank()) {
                    result.add(value);
                }
            }
            return result;
        }
        return List.of();
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpirationSeconds();
    }
}
