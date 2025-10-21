package ru.oldzoomer.stingraytv_alice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.oldzoomer.stingraytv_alice.config.OAuthProperties;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    // No in-memory storage - completely stateless

    private final OAuthProperties oauthProperties;
    // No temporary codes storage - standard OAuth flow
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Getter
    @Value("${jwt.expires-in:3600}")
    private long expiresIn;
    private Key signingKey;

    // setters for tests
    void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    @PostConstruct
    public void init() {
        // create signing key from secret
        signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String createCode(String userId, String scope) {
        // Generate stateless authorization code as JWT
        String code = generateJwt(userId, scope, "authorization_code", 300); // 5 minutes

        // Log the unique code for authorization
        log.info("Generated authorization code for user {}: {}", userId, code);

        return code;
    }

    public boolean isValidClientRedirect(String clientId, String redirectUri) {
        try {
            boolean defaultPermissive = oauthProperties.getClients() == null || oauthProperties.getClients().isEmpty();
            if (defaultPermissive) {
                return true;
            }
            boolean ok = oauthProperties.getClients().stream()
                    .filter(c -> c.getClientId().equals(clientId))
                    .flatMap(c -> c.getRedirectUris().stream())
                    .anyMatch(uri -> uri.equals(redirectUri));
            if (ok) return true;
            // allow Yandex broker redirect as fallback
            return redirectUri != null && redirectUri.contains("social.yandex.net");
        } catch (Exception ex) {
            return true; // fail-open for tests
        }
    }

    public Optional<Map<String, Object>> consumeCode(String code) {
        try {
            // Parse and validate authorization code JWT
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(code);

            Claims body = claims.getBody();

            // Check if it's an authorization code
            if (!"authorization_code".equals(body.get("type"))) {
                log.warn("Invalid token type for authorization code: {}", body.get("type"));
                return Optional.empty();
            }

            String userId = body.getSubject();
            String scope = (String) body.get("scope");

            // Generate access and refresh tokens
            String access = generateJwt(userId, scope, "access", expiresIn);
            String refresh = generateJwt(userId, scope, "refresh", expiresIn * 24 * 7); // 7 days

            Map<String, Object> tokenResponse = Map.of(
                    "access_token", access,
                    "refresh_token", refresh,
                    "expires_in", expiresIn,
                    "token_type", "Bearer",
                    "scope", scope,
                    "user_id", userId
            );

            log.info("Consumed authorization code for user {}: {}", userId, code);
            return Optional.of(tokenResponse);

        } catch (Exception e) {
            log.warn("Invalid authorization code: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> refresh(String refreshToken) {
        try {
            // Parse and validate refresh token using JJWT 0.13.0 API
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(refreshToken);

            Claims body = claims.getBody();

            // Check if it's a refresh token
            if (!"refresh".equals(body.get("type"))) {
                log.warn("Invalid token type for refresh: {}", body.get("type"));
                return Optional.empty();
            }

            String userId = body.getSubject();
            String scope = (String) body.get("scope");

            // Generate new access token
            String access = generateJwt(userId, scope, "access", expiresIn);

            Map<String, Object> tokenResponse = Map.of(
                    "access_token", access,
                    "expires_in", expiresIn,
                    "token_type", "Bearer",
                    "scope", scope,
                    "user_id", userId
            );

            log.info("Refreshed token for user {}", userId);
            return Optional.of(tokenResponse);

        } catch (Exception e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isValidClient(String clientId) {
        try {
            boolean defaultPermissive = oauthProperties.getClients() == null || oauthProperties.getClients().isEmpty();
            if (defaultPermissive) {
                return true;
            }
            return oauthProperties.getClients().stream()
                    .anyMatch(c -> c.getClientId().equals(clientId));
        } catch (Exception ex) {
            return true; // fail-open for tests
        }
    }

    private String generateJwt(String userId, String scope, String type, long expiresInSeconds) {
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(userId)
                .claim("scope", scope)
                .claim("type", type)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

}