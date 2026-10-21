package com.bike.openapi.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 签名工具单元测试。
 */
class SignatureUtilTest {

    private static final String SECRET = "test_secret";
    private static final String METHOD = "POST";
    private static final String PATH = "/api/v3/orders";
    private static final String TIMESTAMP = "1750000000000";
    private static final String NONCE = "a1b2c3d4";

    @Test
    void canonicalQueryShouldSortByNameAndKeepRawEncoding() {
        assertEquals("", SignatureUtil.canonicalQuery(null));
        assertEquals("", SignatureUtil.canonicalQuery(""));
        assertEquals("pageNo=1&pageSize=10", SignatureUtil.canonicalQuery("pageSize=10&pageNo=1"));
        assertEquals("areaCode=110101&name=%E4%B8%9C%E5%9F%8E",
                SignatureUtil.canonicalQuery("name=%E4%B8%9C%E5%9F%8E&areaCode=110101"));
    }

    @Test
    void canonicalStringShouldKeepSixSegments() {
        byte[] body = "{\"sn\":\"BK0001\"}".getBytes(StandardCharsets.UTF_8);
        String canonical = SignatureUtil.canonicalString(METHOD, PATH, null, TIMESTAMP, NONCE, body);

        String[] segments = canonical.split("\n");
        assertEquals(6, segments.length);
        assertEquals(METHOD, segments[0]);
        assertEquals(PATH, segments[1]);
        assertEquals("", segments[2]);
        assertEquals(TIMESTAMP, segments[3]);
        assertEquals(NONCE, segments[4]);
        assertEquals(SignatureUtil.sha256Base64(body), segments[5]);
    }

    @Test
    void emptyBodyHashShouldMatchSha256OfEmptyInput() {
        assertEquals("47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=", SignatureUtil.sha256Base64(null));
        assertEquals("47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=", SignatureUtil.sha256Base64(new byte[0]));
    }

    @Test
    void verifyShouldAcceptCorrectSignatureAndRejectTamperedOne() {
        String canonical = SignatureUtil.canonicalString(METHOD, PATH, null, TIMESTAMP, NONCE, null);
        String signature = SignatureUtil.hmacSha256Base64(SECRET, canonical);

        assertTrue(SignatureUtil.verify(SECRET, canonical, signature));
        assertFalse(SignatureUtil.verify(SECRET, canonical + "x", signature));
        assertFalse(SignatureUtil.verify("other_secret", canonical, signature));
        assertFalse(SignatureUtil.verify(SECRET, canonical, null));
    }
}
