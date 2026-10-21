package com.bike.openapi.security;

import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 缓存请求体，使签名校验与 @RequestBody 可以重复读取。
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedBodyHttpServletRequest(HttpServletRequest request, int maxBodyBytes) throws IOException {
        super(request);
        this.body = readBody(request, maxBodyBytes);
    }

    public byte[] getBody() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(body);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    private static byte[] readBody(HttpServletRequest request, int maxBodyBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = request.getInputStream().read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
            if (buffer.size() > maxBodyBytes) {
                throw new BizException(ErrorCode.PARAM_INVALID, "请求体超过上限");
            }
        }
        return buffer.toByteArray();
    }

    private static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        CachedServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("不支持异步读取");
        }
    }
}
