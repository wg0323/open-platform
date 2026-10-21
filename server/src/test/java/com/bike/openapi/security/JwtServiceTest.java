package com.bike.openapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import com.bike.openapi.admin.auth.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void issuedTokenCarriesIdentity() {
        JwtService service = new JwtService(SECRET, 8);
        String token = service.issue(7L, "alice", 3);
        Claims claims = service.parse(token);

        assertEquals(7L, service.userId(claims));
        assertEquals("alice", service.username(claims));
        assertEquals(3, service.tokenVersion(claims));
        assertFalse(service.jti(claims).isEmpty());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = new JwtService(SECRET, 8);
        String token = service.issue(7L, "alice", 0);

        assertThrows(JwtException.class, () -> service.parse(token + "abc"));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        JwtService service = new JwtService(SECRET, 0);
        String token = service.issue(7L, "alice", 0);
        Thread.sleep(1100L);

        assertThrows(ExpiredJwtException.class, () -> service.parse(token));
    }
}
