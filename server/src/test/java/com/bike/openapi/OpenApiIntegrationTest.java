package com.bike.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接入链路集成测试：用独立实现计算签名，避免与被测工具类互相印证。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiIntegrationTest {

    private static final String APP_KEY = "sbike_demo";
    private static final String APP_SECRET = "SbikeDemoSecret2026x9f8e7d6c5b4a3210";
    private static final String FENCE_PATH = "/api/v3/fences";
    private static final String FENCE_QUERY = "pageNo=1&pageSize=10";
    private static final String ORDER_PATH = "/api/v3/orders";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String CODE_OK = "\"code\":\"0\"";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fencesShouldReturnSeededDataWithValidSignature() {
        ResponseEntity<String> response = exchange(HttpMethod.GET, FENCE_PATH, FENCE_QUERY, null, newNonce(), null);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getBody().contains(CODE_OK), response.getBody());
        assertTrue(response.getBody().contains("东单地铁站"), response.getBody());
    }

    @Test
    void tamperedSignatureShouldBeRejected() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = newNonce();
        HttpHeaders headers = signHeaders(HttpMethod.GET.name(), FENCE_PATH, FENCE_QUERY, null, nonce, timestamp);
        headers.set("X-Signature", headers.getFirst("X-Signature") + "x");

        ResponseEntity<String> response = restTemplate.exchange(url(FENCE_PATH, FENCE_QUERY),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertTrue(response.getStatusCodeValue() == 401);
        assertTrue(response.getBody().contains("GW-40003"), response.getBody());
    }

    @Test
    void replayedNonceShouldBeRejected() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = newNonce();

        ResponseEntity<String> first = exchange(HttpMethod.GET, FENCE_PATH, FENCE_QUERY, null, nonce, null);
        ResponseEntity<String> second = exchange(HttpMethod.GET, FENCE_PATH, FENCE_QUERY, null, nonce, timestamp);

        assertTrue(first.getBody().contains(CODE_OK), first.getBody());
        assertTrue(second.getBody().contains("GW-40005"), second.getBody());
    }

    @Test
    void duplicateOrderShouldBeRejectedByIdempotency() {
        String orderId = "SO" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String body = orderBody(orderId);
        String idempotencyKey = UUID.randomUUID().toString();

        ResponseEntity<String> first = exchange(HttpMethod.POST, ORDER_PATH, null, body, newNonce(), idempotencyKey);
        ResponseEntity<String> second = exchange(HttpMethod.POST, ORDER_PATH, null, body, newNonce(), idempotencyKey);

        assertTrue(first.getBody().contains(CODE_OK), first.getBody());
        assertTrue(second.getBody().contains("BIZ-40901"), second.getBody());
    }

    @Test
    void missingIdempotencyKeyShouldBeRejected() {
        String body = orderBody("SO" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));

        ResponseEntity<String> response = exchange(HttpMethod.POST, ORDER_PATH, null, body, newNonce(), null);

        assertTrue(response.getBody().contains("GW-40008"), response.getBody());
    }

    @Test
    void positionUploadShouldBeIdempotentByNaturalKey() {
        String sn = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String body = "{\"areaCode\":\"110101\",\"sn\":\"" + sn + "\",\"lng\":116.418,\"lat\":39.908,"
                + "\"positionTime\":\"2026-10-21T09:10:00\",\"status\":1,\"busiStatus\":0}";

        ResponseEntity<String> first = exchange(HttpMethod.POST, "/api/v3/positions", null, body, newNonce(), UUID.randomUUID().toString());
        ResponseEntity<String> second = exchange(HttpMethod.POST, "/api/v3/positions", null, body, newNonce(), UUID.randomUUID().toString());

        assertTrue(first.getBody().contains(CODE_OK), first.getBody());
        assertTrue(second.getBody().contains(CODE_OK), second.getBody());
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String query,
                                            String body, String nonce, String idempotencyKey) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        HttpHeaders headers = signHeaders(method.name(), path, query, body, nonce, timestamp);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        return restTemplate.exchange(url(path, query), method, new HttpEntity<>(body, headers), String.class);
    }

    private HttpHeaders signHeaders(String method, String path, String query, String body,
                                    String nonce, String timestamp) {
        String canonical = method + "\n" + path + "\n" + (query == null ? "" : query) + "\n"
                + timestamp + "\n" + nonce + "\n" + sha256Base64(body);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-App-Key", APP_KEY);
        headers.set("X-Timestamp", timestamp);
        headers.set("X-Nonce", nonce);
        headers.set("X-Signature", hmacSha256Base64(canonical));
        return headers;
    }

    private String url(String path, String query) {
        return query == null ? path : path + "?" + query;
    }

    private String orderBody(String orderId) {
        return "{\"areaCode\":\"110101\",\"orderId\":\"" + orderId + "\",\"sn\":\"BK0001\","
                + "\"startTime\":\"2026-10-21T09:00:00\",\"endTime\":\"2026-10-21T09:30:00\",\"amount\":1.50}";
    }

    private String newNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String sha256Base64(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String hmacSha256Base64(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
