package com.healthvault.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mints and verifies stateless JWTs signed with HMAC-SHA256.
 * <p>
 * A JWT is three base64url segments joined by dots: header.payload.signature. The payload carries
 * the user id (sub) and an expiry (exp); the signature is an HMAC over "header.payload" using a
 * server-side secret. Because everything the server needs is inside the token, no session state is
 * stored, so any instance can verify any token. Hand-rolled (no library, tiny JSON built by hand)
 * so the mechanics stay visible.
 */
@Service
public class JwtService {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Pattern SUB = Pattern.compile("\"sub\":(\\d+)");
    private static final Pattern EXP = Pattern.compile("\"exp\":(\\d+)");

    private final byte[] secret;
    private final long ttlSeconds;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.ttl-seconds:3600}") long ttlSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(long userId, String email) {
        long now = Instant.now().getEpochSecond();
        String payload = "{\"sub\":%d,\"email\":\"%s\",\"iat\":%d,\"exp\":%d}"
                .formatted(userId, escape(email), now, now + ttlSeconds);

        String headerB64 = B64.encodeToString(HEADER.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signingInput = headerB64 + "." + payloadB64;
        String signature = B64.encodeToString(hmac(signingInput));
        return signingInput + "." + signature;
    }

    /** Verifies signature and expiry, returning the user id, or throws {@link JwtException}. */
    public long verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtException("malformed token");
        }
        String signingInput = parts[0] + "." + parts[1];
        String expected = B64.encodeToString(hmac(signingInput));
        // constant-time compare to avoid timing side channels
        if (!java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new JwtException("bad signature");
        }
        String payload = new String(B64D.decode(parts[1]), StandardCharsets.UTF_8);
        Matcher exp = EXP.matcher(payload);
        Matcher sub = SUB.matcher(payload);
        if (!exp.find() || !sub.find()) {
            throw new JwtException("unreadable payload");
        }
        if (Instant.now().getEpochSecond() >= Long.parseLong(exp.group(1))) {
            throw new JwtException("token expired");
        }
        return Long.parseLong(sub.group(1));
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static class JwtException extends RuntimeException {
        public JwtException(String message) {
            super(message);
        }
    }
}
