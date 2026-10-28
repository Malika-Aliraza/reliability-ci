package com.example.reliability;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-at-least-32-bytes-long";

    @Test
    void tokenRoundTripKeepsSubjectAndUserId() {
        JwtService service = new JwtService(SECRET, 60_000);
        Claims claims = service.parse(service.generateToken(7L, "a@b.com"));
        assertEquals("a@b.com", claims.getSubject());
        assertEquals(7L, ((Number) claims.get("uid")).longValue());
    }

        @Test
    void tamperedTokenIsRejected() {
        JwtService service = new JwtService(SECRET, 60_000);
        String[] genuine = service.generateToken(1L, "a@b.com").split("\\.");
        String[] other = service.generateToken(2L, "hacker@b.com").split("\\.");
        String forged = genuine[0] + "." + other[1] + "." + genuine[2];   // payload swapped, old signature
        assertThrows(JwtException.class, () -> service.parse(forged));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = new JwtService(SECRET, -1000);
        String token = service.generateToken(1L, "a@b.com");
        assertThrows(JwtException.class, () -> service.parse(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = new JwtService("another-secret-key-that-is-also-32-bytes-long!!", 60_000)
                .generateToken(1L, "a@b.com");
        assertThrows(JwtException.class, () -> new JwtService(SECRET, 60_000).parse(token));
    }
}