package com.linan.web.service;

import java.util.Map;

/**
 * 登录验证码服务
 *
 * <p>生成 SVG 字符串验证码并缓存至 Redis（5 分钟 TTL），
 * 校验时一次性消费（无论对错均删除），从 AuthController 拆分避免控制器过胖。</p>
 *
 * @author BareZen Team
 */
public interface CaptchaService {

    /**
     * 生成验证码
     *
     * @return {@code {uuid, captchaBase64}}：uuid 用于校验回传，base64 为 SVG 图片数据
     */
    Map<String, String> generate();

    /**
     * 校验并消费验证码
     *
     * @param uuid 获取验证码时返回的 uuid
     * @param code 用户输入的验证码
     * @return 校验通过返回 {@code null}，否则返回错误提示文案
     */
    String validate(String uuid, String code);
}