package com.bike.openapi.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 签名工具：待签串为 方法、路径、规范化 query、时间戳、nonce、body 哈希 六段，用 LF 连接。
 */
public final class SignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA_256 = "SHA-256";
    private static final String LF = "\n";
    private static final String AMP = "&";
    private static final String EQ = "=";

    private SignatureUtil() {
    }

    /**
     * 构造待签串。
     *
     * @param method         请求方法
     * @param path           请求路径
     * @param rawQuery       原始 query，可为 null
     * @param timestamp      时间戳
     * @param nonce          随机串
     * @param body           请求体，可为 null
     * @return 待签串
     */
    public static String canonicalString(String method, String path, String rawQuery,
                                         String timestamp, String nonce, byte[] body) {
        return method.toUpperCase() + LF
                + path + LF
                + canonicalQuery(rawQuery) + LF
                + timestamp + LF
                + nonce + LF
                + sha256Base64(body);
    }

    /**
     * query 参数按参数名排序后原样拼接，不做解码，保证两端规则一致。
     *
     * @param rawQuery 原始 query
     * @return 规范化 query
     */
    public static String canonicalQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        return Arrays.stream(rawQuery.split(AMP))
                .filter(item -> !item.isEmpty())
                .sorted(Comparator.comparing(SignatureUtil::paramName))
                .collect(Collectors.joining(AMP));
    }

    /**
     * 计算 body 的 SHA-256 并 Base64 编码。
     *
     * @param body 请求体，可为 null
     * @return Base64 字符串
     */
    public static String sha256Base64(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(body == null ? new byte[0] : body);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    /**
     * HMAC-SHA256 后 Base64 编码。
     *
     * @param secret 密钥
     * @param data   数据
     * @return Base64 签名
     */
    public static String hmacSha256Base64(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("签名计算失败", exception);
        }
    }

    /**
     * 校验签名，使用时间无关比较避免旁路攻击。
     *
     * @param secret    密钥
     * @param data      待签串
     * @param signature 客户端签名
     * @return 是否一致
     */
    public static boolean verify(String secret, String data, String signature) {
        if (signature == null) {
            return false;
        }
        String expected = hmacSha256Base64(secret, data);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private static String paramName(String item) {
        int index = item.indexOf(EQ);
        return index < 0 ? item : item.substring(0, index);
    }
}
