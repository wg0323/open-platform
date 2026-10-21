package com.bike.sdk;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 签名拦截器：为每个请求生成时间戳、nonce 与 HMAC-SHA256 签名，调用方无需自行实现签名。
 */
public final class HmacSigningInterceptor implements Interceptor {

    private static final String HEADER_APP_KEY = "X-App-Key";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGNATURE = "X-Signature";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA_256 = "SHA-256";
    private static final String LF = "\n";
    private static final String AMP = "&";
    private static final String EQ = "=";

    private final String appKey;
    private final String appSecret;

    public HmacSigningInterceptor(String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl url = request.url();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String canonical = request.method().toUpperCase() + LF
                + url.encodedPath() + LF
                + canonicalQuery(url.encodedQuery()) + LF
                + timestamp + LF
                + nonce + LF
                + sha256Base64(readBody(request));
        Request signed = request.newBuilder()
                .header(HEADER_APP_KEY, appKey)
                .header(HEADER_TIMESTAMP, timestamp)
                .header(HEADER_NONCE, nonce)
                .header(HEADER_SIGNATURE, hmacSha256Base64(canonical))
                .build();
        return chain.proceed(signed);
    }

    private static String canonicalQuery(String encodedQuery) {
        if (encodedQuery == null || encodedQuery.isEmpty()) {
            return "";
        }
        List<String> items = new ArrayList<>();
        for (String item : encodedQuery.split(AMP)) {
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        items.sort(Comparator.comparing(HmacSigningInterceptor::paramName));
        return String.join(AMP, items);
    }

    private static String paramName(String item) {
        int index = item.indexOf(EQ);
        return index < 0 ? item : item.substring(0, index);
    }

    private static byte[] readBody(Request request) throws IOException {
        RequestBody body = request.body();
        if (body == null) {
            return new byte[0];
        }
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readByteArray();
    }

    private static String sha256Base64(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            return Base64.getEncoder().encodeToString(digest.digest(body));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private String hmacSha256Base64(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("签名计算失败", exception);
        }
    }
}
