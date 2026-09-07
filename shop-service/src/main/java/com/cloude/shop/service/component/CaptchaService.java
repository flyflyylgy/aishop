package com.cloude.shop.service.component;

import com.cloude.shop.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 图形验证码：JDK AWT 自绘，答案存 Redis（5 分钟有效，验证即焚）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptchaService {

    private static final char[] CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final Random random = new Random();

    /**
     * 生成验证码：返回 captchaKey 与 base64 图片
     */
    public Map<String, String> generate() {
        String code = randomCode(4);
        String key = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(RedisKeyConstant.CAPTCHA_PREFIX + key, code, TTL);

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", key);
        result.put("img", "data:image/png;base64," + draw(code));
        return result;
    }

    /**
     * 校验验证码（无论对错一次性失效，防暴力枚举）
     */
    public boolean verify(String captchaKey, String input) {
        if (captchaKey == null || input == null) {
            return false;
        }
        String redisKey = RedisKeyConstant.CAPTCHA_PREFIX + captchaKey;
        String expected = stringRedisTemplate.opsForValue().get(redisKey);
        stringRedisTemplate.delete(redisKey);
        return expected != null && expected.equalsIgnoreCase(input.trim());
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    private String draw(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // 背景
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(random.nextInt(180), random.nextInt(180), random.nextInt(180)));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
        // 字符
        g.setFont(new Font("Arial", Font.BOLD, 26));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(20 + random.nextInt(110), 20 + random.nextInt(110), 20 + random.nextInt(110)));
            int x = 18 + i * 24;
            int y = 28 + random.nextInt(6) - 3;
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }
        // 噪点
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
        }
        g.dispose();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("验证码图片生成失败", e);
            throw new IllegalStateException("验证码生成失败");
        }
    }
}
