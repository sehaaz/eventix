package com.sehaaz.eventtix.auth.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HS256 JWT üretimi. Gateway'deki JwtVerifier ile aynı format: sub = userId, role, exp (saniye).
 */
@Component
public class JwtIssuer {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Duration TTL = Duration.ofHours(1);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String HEADER = ENCODER.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final SecretKeySpec key;
    private final ObjectMapper mapper;
    private final Clock clock;

    public JwtIssuer(@Value("${jwt.secret}") String secret, ObjectMapper mapper, Clock clock) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret en az 32 byte olmalı");
        }
        this.key = new SecretKeySpec(secretBytes, ALGORITHM);
        this.mapper = mapper;
        this.clock = clock;
    }

    public String issue(User user) {
        long now = clock.instant().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(user.getId()));
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("iat", now);
        claims.put("exp", now + TTL.toSeconds());

        try {
            String signingInput = HEADER + "." + ENCODER.encodeToString(mapper.writeValueAsBytes(claims));
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + ENCODER.encodeToString(signature);
        } catch (JsonProcessingException | GeneralSecurityException e) {
            throw new IllegalStateException("JWT üretilemedi", e);
        }
    }
}
