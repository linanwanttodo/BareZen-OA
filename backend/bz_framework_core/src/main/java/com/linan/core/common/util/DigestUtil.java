package com.linan.core.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 摘要工具：统一 SHA-256 十六进制计算
 *
 * <p>收敛散落在 JWT 黑名单 / API Key 哈希 / 防重复提交指纹中的重复实现，
 * 便于统一算法与异常策略（标准 JVM 上 SHA-256 必然可用）。</p>
 *
 * @author BareZen Team
 */
public final class DigestUtil {

    private static final String SHA_256 = "SHA-256";

    private DigestUtil() {}

    /** 计算字符串 UTF-8 字节的 SHA-256 十六进制小写摘要 */
    public static String sha256Hex(String input) {
        return sha256Hex(input.getBytes(StandardCharsets.UTF_8));
    }

    /** 计算字节数组的 SHA-256 十六进制小写摘要 */
    public static String sha256Hex(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA_256);
            byte[] hash = md.digest(input);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // 标准 JVM 必然提供 SHA-256；此处仅防御性兜底
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}