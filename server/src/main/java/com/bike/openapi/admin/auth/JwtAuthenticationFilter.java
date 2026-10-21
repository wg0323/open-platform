package com.bike.openapi.admin.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.common.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 管理台认证：解析 Bearer token、校验黑名单与用户状态、装配权限。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final int TOKEN_BEGIN = 7;

    /** 强制改密期间仍可访问的接口。 */
    private static final Set<String> PASSWORD_CHANGE_ALLOWED = new HashSet<>(Arrays.asList(
            "/admin/api/auth/me", "/admin/api/auth/password",
            "/admin/api/auth/logout", "/admin/api/auth/captcha"));

    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final LoginUserService loginUserService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }
        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (JwtException | IllegalArgumentException exception) {
            writeError(response, ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
            return;
        }
        if (blacklistService.isBlacklisted(jwtService.jti(claims))) {
            writeError(response, ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
            return;
        }
        LoginUser loginUser = loginUserService.loadByUserId(jwtService.userId(claims));
        if (loginUser == null || !loginUser.isEnabled()
                || loginUser.getTokenVersion() != jwtService.tokenVersion(claims)) {
            writeError(response, ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
            return;
        }
        if (loginUser.isMustChangePassword() && !PASSWORD_CHANGE_ALLOWED.contains(request.getServletPath())) {
            writeError(response, ErrorCode.PASSWORD_CHANGE_REQUIRED, null);
            return;
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }
        String token = header.substring(TOKEN_BEGIN).trim();
        return token.isEmpty() ? null : token;
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResult.fail(errorCode, message, null)));
    }
}
