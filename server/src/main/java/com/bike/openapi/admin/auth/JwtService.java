package com.bike.openapi.admin.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析。载荷只放身份，权限每次请求从库装配。
 */
@Component
public class JwtService {

    private static final long SECONDS_PER_HOUR = 3600L;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_TOKEN_VERSION = "tv";

    private final SecretKey key;
    private final long expireHours;

    public JwtService(@Value("${admin.jwt.secret}") String secret,
                      @Value("${admin.jwt.expire-hours:8}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireHours = expireHours;
    }

    /**
     * 签发 token。
     *
     * @param userId       用户主键
     * @param username     登录账号
     * @param tokenVersion 用户当前的 token 版本
     * @return JWT 字符串
     */
    public String issue(Long userId, String username, int tokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireHours * SECONDS_PER_HOUR * MILLIS_PER_SECOND);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并校验签名与有效期。
     *
     * @param token JWT 字符串
     * @return 载荷
     */
    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public Long userId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String username(Claims claims) {
        return claims.get(CLAIM_USERNAME, String.class);
    }

    public int tokenVersion(Claims claims) {
        Integer value = claims.get(CLAIM_TOKEN_VERSION, Integer.class);
        return value == null ? 0 : value;
    }

    public String jti(Claims claims) {
        return claims.getId();
    }

    /**
     * 剩余有效秒数，用于黑名单 TTL。
     *
     * @param claims 载荷
     * @return 剩余秒数
     */
    public long remainingSeconds(Claims claims) {
        long remain = (claims.getExpiration().getTime() - System.currentTimeMillis()) / MILLIS_PER_SECOND;
        return Math.max(remain, 0);
    }
}
