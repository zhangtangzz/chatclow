package com.chatclow.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

/**
 * JWT 工具类
 * 负责：生成 Token、解析 Token、验证 Token
 */
public class JwtUtil {

    // 签名密钥（实际项目应该放在配置文件里，不要硬编码）
    private static final String SECRET_KEY = "chatclow-2026-secret-key-must-be-long-enough";

    // Token 有效期：24 小时（单位：毫秒）
    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000L;
    // 刷新 Token 有效期：7 天
    private static final long REFRESH_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 生成 Token
     * @param userId 用户ID
     * @param username 用户名
     * @return 生成的 Token 字符串
     */
    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                // 存入用户信息
                .claim("userId", userId)
                .claim("username", username)
                // 设置签发时间
                .setIssuedAt(new Date())
                // 设置过期时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                // 设置签名算法和密钥
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    /**
     * 解析 Token，获取 Claims（里面存着之前放进去的用户信息）
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Token 中获取用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 生成 Refresh Token（7 天有效期）
     */
    public static String generateRefreshToken(Long userId, String username) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRE_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    /**
     * 验证 Token 是否有效（未过期且签名正确）
     * 如果无效会抛异常，返回 true 表示有效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
