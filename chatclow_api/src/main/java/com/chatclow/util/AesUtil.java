package com.chatclow.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加密/解密工具
 * 用于加密存储 AI 模型的 API 密钥
 */
public class AesUtil {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    private static String secretKey;

    /**
     * 由 Spring 启动时注入（从 application.yml 读取 crypto.key）
     */
    public static void setSecretKey(String key) {
        if (key.length() < 16) {
            throw new RuntimeException("crypto.key 长度不能小于 16 位");
        }
        secretKey = key;
    }

    private static SecretKeySpec getKey() {
        if (secretKey == null) {
            throw new RuntimeException("AES 密钥未初始化，请在 application.yml 中配置 crypto.key");
        }
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        // 取前 16 字节作为 AES-128 密钥
        byte[] key16 = new byte[16];
        System.arraycopy(keyBytes, 0, key16, 0, Math.min(keyBytes.length, 16));
        return new SecretKeySpec(key16, "AES");
    }

    public static String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    /**
     * 脱敏：只保留前 4 位，其余用 * 代替
     */
    public static String mask(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return apiKey;
        }
        return apiKey.substring(0, 4) + "****";
    }

    /**
     * 判断是否为脱敏后的密钥（编辑时未修改）
     */
    public static boolean isMasked(String apiKey) {
        return apiKey != null && apiKey.contains("****");
    }
}
