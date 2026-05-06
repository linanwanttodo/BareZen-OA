package com.linan.web.service.impl;

import com.linan.web.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录验证码服务实现
 *
 * <p>代码从 AuthController 迁出：验证码字符集排除易混淆字符（0/O/1/I），
 * SVG 渲染带噪点/干扰线/字符旋转，防 OCR 识别。</p>
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    private static final long CAPTCHA_TTL_MINUTES = 5;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, String> generate() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String code = generateCode(6);

        redisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + uuid, code,
                CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);

        Map<String, String> data = new HashMap<>();
        data.put("uuid", uuid);
        data.put("captchaBase64", "data:image/svg+xml;base64," + generateSvgCaptcha(code));
        return data;
    }

    @Override
    public String validate(String uuid, String code) {
        if (uuid == null || uuid.isEmpty() || code == null || code.isEmpty()) {
            return "验证码不能为空";
        }
        String redisKey = CAPTCHA_KEY_PREFIX + uuid;
        Object cachedCode = redisTemplate.opsForValue().get(redisKey);
        // 验证码一次性使用，无论对错都删除
        redisTemplate.delete(redisKey);
        if (cachedCode == null) {
            return "验证码已过期，请重新获取";
        }
        if (!cachedCode.toString().equalsIgnoreCase(code)) {
            return "验证码错误";
        }
        return null;
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS.charAt(SECURE_RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成带干扰线、噪点和字符旋转扭曲的 SVG 验证码，防止 OCR 识别
     */
    private String generateSvgCaptcha(String code) {
        SecureRandom random = SECURE_RANDOM;
        int width = 160;
        int height = 55;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(width).append("' height='").append(height).append("'>");

        // 背景
        svg.append("<rect fill='#f0f0f0' width='").append(width).append("' height='").append(height).append("'/>");

        // 噪点 — 随机小圆点
        for (int i = 0; i < 150; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            String color = randomRgb(random);
            svg.append("<circle cx='").append(x).append("' cy='").append(y).append("' r='").append(random.nextBoolean() ? 1 : 2).append("' fill='").append(color).append("' opacity='0.7'/>");
        }

        // 干扰线 — 8条随机曲线
        for (int i = 0; i < 8; i++) {
            String color = randomRgb(random);
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            int cx1 = random.nextInt(width);
            int cy1 = random.nextInt(height);
            svg.append("<path d='M").append(x1).append(",").append(y1)
               .append(" Q").append(cx1).append(",").append(cy1)
               .append(" ").append(x2).append(",").append(y2)
               .append("' stroke='").append(color).append("' stroke-width='1.5' fill='none' opacity='0.5'/>");
        }

        // 字符 — 每个字符随机旋转、偏移、颜色
        String[] colors = {"#333", "#2a5", "#c25", "#25c", "#c82", "#52c"};
        int charWidth = (width - 30) / code.length();
        for (int i = 0; i < code.length(); i++) {
            int x = 18 + i * charWidth + random.nextInt(8);
            int y = 30 + random.nextInt(12) - 6;
            int rotate = random.nextInt(40) - 20; // -20° ~ 20°
            String color = colors[random.nextInt(colors.length)];
            int fontSize = 22 + random.nextInt(6);
            svg.append("<text x='").append(x).append("' y='").append(y)
               .append("' font-size='").append(fontSize)
               .append("' fill='").append(color)
               .append("' font-family='monospace' font-weight='bold'")
               .append(" transform='rotate(").append(rotate).append(",").append(x).append(",").append(y).append(")'")
               .append(">").append(code.charAt(i)).append("</text>");
        }

        // 额外干扰弧线
        for (int i = 0; i < 5; i++) {
            String color = randomRgb(random);
            svg.append("<line x1='").append(random.nextInt(width)).append("' y1='").append(random.nextInt(height))
               .append("' x2='").append(random.nextInt(width)).append("' y2='").append(random.nextInt(height))
               .append("' stroke='").append(color).append("' stroke-width='1' opacity='0.4'/>");
        }

        svg.append("</svg>");

        try {
            return Base64.getEncoder().encodeToString(svg.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(svg.toString().getBytes());
        }
    }

    private String randomRgb(SecureRandom random) {
        int r = random.nextInt(150) + 50;
        int g = random.nextInt(150) + 50;
        int b = random.nextInt(150) + 50;
        return "rgb(" + r + "," + g + "," + b + ")";
    }
}