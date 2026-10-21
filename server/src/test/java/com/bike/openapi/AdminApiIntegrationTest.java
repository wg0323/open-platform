package com.bike.openapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 管理台集成测试：验证码、登录、强制改密、RBAC、登出失效。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminApiIntegrationTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String CHANGED_PASSWORD = "Admin@2026";
    private static final String CODE_OK = "\"code\":\"0\"";
    private static final String CAPTCHA_KEY_PREFIX = "admin:captcha:";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 每个用例前把 admin 恢复为初始状态，并清理测试用户，保证可重复运行。
     */
    @BeforeEach
    void resetState() {
        jdbcTemplate.update("UPDATE sys_user SET password = ?, must_change_password = 1, token_version = 0, enabled = 1 "
                + "WHERE username = ?", passwordEncoder.encode(ADMIN_PASSWORD), ADMIN_USERNAME);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN "
                + "(SELECT id FROM sys_user WHERE username LIKE 'it_%')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'it_%'");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setOutputStreaming(false);
        restTemplate.getRestTemplate().setRequestFactory(factory);
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity("/admin/api/apps", String.class);

        assertEquals(401, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("ADM-40100"), response.getBody());
    }

    @Test
    void wrongCaptchaShouldBeRejected() {
        String captchaId = captchaId();
        String body = "{\"captchaId\":\"" + captchaId + "\",\"captchaCode\":\"ZZZZ\","
                + "\"username\":\"admin\",\"password\":\"Admin@123\"}";

        ResponseEntity<String> response = postJson("/admin/api/auth/login", body, null);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("ADM-40001"), response.getBody());
    }

    @Test
    void firstLoginRequiresPasswordChangeThenWorks() {
        String firstToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        assertTrue(firstToken.length() > 0, "登录应返回 token");

        ResponseEntity<String> blocked = exchange(HttpMethod.GET, "/admin/api/apps", null, firstToken);
        assertEquals(403, blocked.getStatusCodeValue());
        assertTrue(blocked.getBody().contains("ADM-40301"), blocked.getBody());

        ResponseEntity<String> changed = changePassword(firstToken, ADMIN_PASSWORD, CHANGED_PASSWORD);
        assertTrue(changed.getBody().contains(CODE_OK), changed.getBody());

        ResponseEntity<String> stale = exchange(HttpMethod.GET, "/admin/api/apps", null, firstToken);
        assertEquals(401, stale.getStatusCodeValue(), "改密后旧 token 应失效");

        String token = login(ADMIN_USERNAME, CHANGED_PASSWORD);
        ResponseEntity<String> list = exchange(HttpMethod.GET, "/admin/api/apps", null, token);
        assertTrue(list.getBody().contains(CODE_OK), list.getBody());
        assertTrue(list.getBody().contains("sbike_demo"), list.getBody());

        ResponseEntity<String> metrics = exchange(HttpMethod.GET, "/admin/api/metrics", null, token);
        assertTrue(metrics.getBody().contains(CODE_OK), metrics.getBody());
    }

    @Test
    void logoutInvalidatesToken() {
        String token = loginAfterChange();

        ResponseEntity<String> logout = exchange(HttpMethod.POST, "/admin/api/auth/logout", null, token);
        assertTrue(logout.getBody().contains(CODE_OK), logout.getBody());

        ResponseEntity<String> after = exchange(HttpMethod.GET, "/admin/api/apps", null, token);
        assertEquals(401, after.getStatusCodeValue(), "登出后 token 应立即失效");
    }

    @Test
    void operatorCannotAccessUserApi() {
        String adminToken = loginAfterChange();
        ResponseEntity<String> created = exchange(HttpMethod.POST, "/admin/api/users",
                "{\"username\":\"it_operator\",\"password\":\"Operator123\","
                        + "\"nickname\":\"集成测试运营\",\"roleIds\":[2]}", adminToken);
        assertTrue(created.getBody().contains(CODE_OK), created.getBody());

        String operatorToken = login("it_operator", "Operator123");
        changePassword(operatorToken, "Operator123", "Operator2026");
        operatorToken = login("it_operator", "Operator2026");

        ResponseEntity<String> apps = exchange(HttpMethod.GET, "/admin/api/apps", null, operatorToken);
        assertTrue(apps.getBody().contains(CODE_OK), apps.getBody());

        ResponseEntity<String> users = exchange(HttpMethod.GET, "/admin/api/users", null, operatorToken);
        assertEquals(403, users.getStatusCodeValue());
        assertTrue(users.getBody().contains("ADM-40300"), users.getBody());

        ResponseEntity<String> me = exchange(HttpMethod.GET, "/admin/api/auth/me", null, operatorToken);
        assertTrue(me.getBody().contains("指标看板"), me.getBody());
        assertFalse(me.getBody().contains("系统管理"), me.getBody());
    }

    private String loginAfterChange() {
        String firstToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        changePassword(firstToken, ADMIN_PASSWORD, CHANGED_PASSWORD);
        return login(ADMIN_USERNAME, CHANGED_PASSWORD);
    }

    private String login(String username, String password) {
        String captchaId = captchaId();
        String body = "{\"captchaId\":\"" + captchaId + "\",\"captchaCode\":\""
                + redisTemplate.opsForValue().get(CAPTCHA_KEY_PREFIX + captchaId) + "\","
                + "\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        return extractString(postJson("/admin/api/auth/login", body, null).getBody(), "\"token\":\"");
    }

    private ResponseEntity<String> changePassword(String token, String oldPassword, String newPassword) {
        String body = "{\"oldPassword\":\"" + oldPassword + "\",\"newPassword\":\"" + newPassword
                + "\",\"confirmPassword\":\"" + newPassword + "\"}";
        return exchange(HttpMethod.PUT, "/admin/api/auth/password", body, token);
    }

    private String captchaId() {
        ResponseEntity<String> response = restTemplate.getForEntity("/admin/api/auth/captcha", String.class);
        return extractString(response.getBody(), "\"captchaId\":\"");
    }

    private ResponseEntity<String> postJson(String path, String body, String token) {
        return exchange(HttpMethod.POST, path, body, token);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private String extractString(String json, String prefix) {
        int start = json.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        int from = start + prefix.length();
        int end = json.indexOf('"', from);
        return json.substring(from, end);
    }
}
