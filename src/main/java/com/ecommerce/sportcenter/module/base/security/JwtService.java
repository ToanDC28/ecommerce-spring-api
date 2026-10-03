package com.ecommerce.sportcenter.module.base.security;

import com.ecommerce.sportcenter.module.role.entity.Permission;
import com.ecommerce.sportcenter.module.role.entity.Role;
import com.ecommerce.sportcenter.module.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-ms:}") String accessExpiration,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs,
            @Value("${app.jwt.expiration-ms:900000}") long legacyExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpiration != null && !accessExpiration.isBlank()
                ? Long.parseLong(accessExpiration)
                : legacyExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(User user) {
        Set<String> roles = user.getRoles() == null ? Set.of()
                : user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        Set<String> permissions = user.getRoles() == null ? Set.of()
                : user.getRoles().stream()
                        .filter(r -> r.getPermissions() != null)
                        .flatMap(r -> r.getPermissions().stream())
                        .map(Permission::getName)
                        .collect(Collectors.toSet());
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("email", user.getEmail());
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("authorities", user.getAuthorities().stream()
                .map(Object::toString).toList());
        return buildToken(claims, user.getUsername(), UUID.randomUUID().toString(), accessExpirationMs);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, user.getUsername(), UUID.randomUUID().toString(), refreshExpirationMs);
    }

    /** Backwards-compatible: generates an access token. */
    public String generateToken(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return generateAccessToken(user);
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(Object::toString).toList());
        return buildToken(claims, userDetails.getUsername(), UUID.randomUUID().toString(), accessExpirationMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, String jti, long expirationMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, c -> c.get("type", String.class));
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        return "access".equals(extractTokenType(token)) && isTokenValid(token, userDetails);
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        return "refresh".equals(extractTokenType(token)) && isTokenValid(token, userDetails);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
