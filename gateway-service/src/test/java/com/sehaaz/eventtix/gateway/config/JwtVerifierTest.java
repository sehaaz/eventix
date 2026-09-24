package com.sehaaz.eventtix.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtVerifierTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String HS256_HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final JwtVerifier verifier =
            new JwtVerifier(SECRET, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void validToken_returnsClaims() {
        String token = sign(HS256_HEADER, payload("42", "USER", NOW.plusSeconds(60)), SECRET);

        assertThat(verifier.verify(token)).contains(new JwtVerifier.Claims("42", "USER"));
    }

    @Test
    void wrongSecret_isRejected() {
        String token = sign(HS256_HEADER, payload("42", "USER", NOW.plusSeconds(60)), SECRET + "x");

        assertThat(verifier.verify(token)).isEmpty();
    }

    @Test
    void tamperedPayload_isRejected() {
        String token = sign(HS256_HEADER, payload("42", "USER", NOW.plusSeconds(60)), SECRET);
        String[] parts = token.split("\\.");
        String forged = parts[0] + "." + encode(payload("42", "ADMIN", NOW.plusSeconds(60))) + "." + parts[2];

        assertThat(verifier.verify(forged)).isEmpty();
    }

    @Test
    void expiredToken_isRejected() {
        String token = sign(HS256_HEADER, payload("42", "USER", NOW), SECRET);

        assertThat(verifier.verify(token)).isEmpty();
    }

    @Test
    void algNone_isRejected() {
        String token = encode("{\"alg\":\"none\"}") + "." + encode(payload("42", "ADMIN", NOW.plusSeconds(60))) + ".";

        assertThat(verifier.verify(token)).isEmpty();
    }

    @Test
    void missingRole_isRejected() {
        String token = sign(HS256_HEADER, "{\"sub\":\"42\",\"exp\":" + NOW.plusSeconds(60).getEpochSecond() + "}", SECRET);

        assertThat(verifier.verify(token)).isEmpty();
    }

    @Test
    void garbage_isRejected() {
        assertThat(verifier.verify("not-a-jwt")).isEmpty();
        assertThat(verifier.verify("a.b.c")).isEmpty();
    }

    @Test
    void shortSecret_failsFast() {
        assertThatThrownBy(() -> new JwtVerifier("short", new ObjectMapper(), Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String payload(String sub, String role, Instant exp) {
        return "{\"sub\":\"" + sub + "\",\"role\":\"" + role + "\",\"exp\":" + exp.getEpochSecond() + "}";
    }

    private static String sign(String header, String payload, String secret) {
        try {
            String signingInput = encode(header) + "." + encode(payload);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
