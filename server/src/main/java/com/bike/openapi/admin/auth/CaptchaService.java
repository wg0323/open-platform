package com.bike.openapi.admin.auth;

import lombok.RequiredArgsConstructor;
import com.bike.openapi.admin.auth.dto.CaptchaVo;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码：Java2D 绘制，答案存 Redis，校验即删。
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String KEY_PREFIX = "admin:captcha:";
    private static final Duration TTL = Duration.ofSeconds(120);
    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int LINE_COUNT = 6;
    private static final int NOISE_COUNT = 30;
    private static final int FONT_SIZE = 28;
    private static final int CHAR_STEP = 26;
    private static final int CHAR_OFFSET = 12;
    private static final int BASELINE = 29;
    private static final int COLOR_MIN = 30;
    private static final int COLOR_MAX = 150;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();

    /**
     * 生成验证码，答案写入 Redis，图片转 base64 data URL。
     *
     * @return 验证码标识与图片
     */
    public CaptchaVo generate() {
        String code = randomCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(KEY_PREFIX + captchaId, code, TTL);
        return new CaptchaVo(captchaId, "data:image/png;base64," + draw(code));
    }

    /**
     * 校验并删除验证码，一次性消费。
     *
     * @param captchaId 验证码标识
     * @param code      用户输入
     */
    public void validate(String captchaId, String code) {
        if (captchaId == null || code == null) {
            throw new BizException(ErrorCode.CAPTCHA_INVALID);
        }
        String key = KEY_PREFIX + captchaId;
        String expected = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (expected == null || !expected.equalsIgnoreCase(code.trim())) {
            throw new BizException(ErrorCode.CAPTCHA_INVALID);
        }
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return builder.toString();
    }

    private String draw(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(245, 247, 250));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        for (int i = 0; i < LINE_COUNT; i++) {
            graphics.setColor(randomColor(150, 220));
            graphics.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
        for (int i = 0; i < NOISE_COUNT; i++) {
            graphics.setColor(randomColor(120, 220));
            graphics.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }
        graphics.setFont(new Font("SansSerif", Font.BOLD, FONT_SIZE));
        for (int i = 0; i < code.length(); i++) {
            Graphics2D charGraphics = (Graphics2D) graphics.create();
            charGraphics.setColor(randomColor(COLOR_MIN, COLOR_MAX));
            double angle = (random.nextDouble() - 0.5) * 0.5;
            charGraphics.rotate(angle, CHAR_OFFSET + i * CHAR_STEP, HEIGHT / 2.0);
            charGraphics.drawString(String.valueOf(code.charAt(i)), CHAR_OFFSET + i * CHAR_STEP, BASELINE);
            charGraphics.dispose();
        }
        graphics.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException exception) {
            throw new UncheckedIOException("生成验证码失败", exception);
        }
    }

    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range));
    }
}
