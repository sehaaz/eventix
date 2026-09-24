package com.sehaaz.eventtix.gateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;

/**
 * HS256 JWT doğrulaması: imza, alg ve exp kontrol edilir; sub = userId, role = rol.
 */
public class JwtVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final SecretKeySpec key;
    private final ObjectMapper mapper;
    private final Clock clock;

    public JwtVerifier(String secret, ObjectMapper mapper, Clock clock) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret en az 32 byte olmalı");
        }
        this.key = new SecretKeySpec(secretBytes, ALGORITHM);
        this.mapper = mapper;
        this.clock = clock;
    }

    public Optional<Claims> verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                return Optional.empty();
            }

            JsonNode header = mapper.readTree(DECODER.decode(parts[0]));
            if (!"HS256".equals(header.path("alg").textValue())) {
                return Optional.empty();
            }

            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] expected = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!MessageDigest.isEqual(expected, DECODER.decode(parts[2]))) {
                return Optional.empty();
            }

            JsonNode payload = mapper.readTree(DECODER.decode(parts[1]));
            JsonNode exp = payload.get("exp");
            if (exp == null || !exp.canConvertToLong() || clock.instant().getEpochSecond() >= exp.asLong()) {
                return Optional.empty();
            }

            String userId = payload.path("sub").textValue();
            String role = payload.path("role").textValue();
            if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new Claims(userId, role));
        } catch (IllegalArgumentException | IOException | GeneralSecurityException e) {
            return Optional.empty();
        }
    }

    public record Claims(String userId, String role) {
    }
}
