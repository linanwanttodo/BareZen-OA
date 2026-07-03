package com.linan.web.service;

import com.linan.web.service.impl.CaptchaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CaptchaServiceImpl 登录验证码单测：生成入缓存、一次性校验消费
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private CaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        // lenient：blank 输入分支断言"不触达 Redis"，该 stub 在那些用例中允许未被使用
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("generate：随机 6 位验证码入 Redis（5 分钟 TTL），返回 uuid 与 SVG 图片")
    void generate_storesCodeAndReturnsBase64() {
        Map<String, String> data = captchaService.generate();

        assertTrue(data.containsKey("uuid"));
        assertTrue(data.get("captchaBase64").startsWith("data:image/svg+xml;base64,"));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(eq("captcha:" + data.get("uuid")), codeCaptor.capture(),
                eq(5L), eq(TimeUnit.MINUTES));
        assertEquals(6, codeCaptor.getValue().length());
        // 验证码字符集不含易混淆字符 0/O/1/I
        assertTrue(codeCaptor.getValue().matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}"));
    }

    @Test
    @DisplayName("validate：大小写不敏感匹配通过，且一次性删除缓存")
    void validate_correctCode_consumed() {
        when(valueOps.get("captcha:abc")).thenReturn("AB12CD");

        assertNull(captchaService.validate("abc", "ab12cd"));

        verify(redisTemplate).delete("captcha:abc");
    }

    @Test
    @DisplayName("validate：验证码错误返回提示并同样删除缓存")
    void validate_wrongCode_rejected() {
        when(valueOps.get("captcha:abc")).thenReturn("AB12CD");

        assertEquals("验证码错误", captchaService.validate("abc", "WRONG"));
        verify(redisTemplate).delete("captcha:abc");
    }

    @Test
    @DisplayName("validate：缓存缺失视为过期")
    void validate_missingCache_expired() {
        when(valueOps.get("captcha:abc")).thenReturn(null);

        assertEquals("验证码已过期，请重新获取", captchaService.validate("abc", "X"));
        verify(redisTemplate).delete("captcha:abc");
    }

    @Test
    @DisplayName("validate：uuid 或 code 为空直接拒绝，不触达 Redis")
    void validate_blankInput_rejected() {
        assertEquals("验证码不能为空", captchaService.validate(null, "123"));
        assertEquals("验证码不能为空", captchaService.validate("abc", ""));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("generate：字形以 x 居中且落在安全边距内（避免末位贴边）")
    void generate_glyphsCenteredWithMargins() {
        final int width = 160;
        // 排布含随机抖动与旋转，重复取样才能覆盖最不利的组合
        for (int round = 0; round < 300; round++) {
            String svg = new String(java.util.Base64.getDecoder().decode(
                    captchaService.generate().get("captchaBase64").split(",")[1]),
                    java.nio.charset.StandardCharsets.UTF_8);
            var matcher = java.util.regex.Pattern.compile("<text[^>]*>").matcher(svg);
            int seen = 0;
            while (matcher.find()) {
                seen++;
                String tag = matcher.group();
                var xm = java.util.regex.Pattern.compile("x='(\\d+)'").matcher(tag);
                assertTrue(xm.find(), "字符标签缺少 x 坐标: " + tag);
                int x = Integer.parseInt(xm.group(1));
                // 居中锚点：字形向两侧均匀伸展，旋转后不会整体右偏贴边
                assertTrue(tag.contains("text-anchor='middle'"),
                        "字形必须居中锚定，实际标签: " + tag);
                assertTrue(x >= 22 && x <= width - 22,
                        "字符中心 x 需留在 22px 安全边距内，实际 x=" + x);
            }
            assertEquals(6, seen, "每次应绘制 6 个字符");
        }
    }
}
