package com.yingjianxia.common.core.utils;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWT Token 工具类
 * <p>
 * 支持：Access Token 生成 / 解析 / 验签、双 Token 机制、Claims 注入。
 * <p>
 * 配置前缀：{@code yingjianxia.jwt}
 * </p>
 * <p>
 * 标准 Claims 字段约定：
 * <ul>
 *   <li>{@code sub}    : 用户ID（userId）</li>
 *   <li>{@code roles}  : 角色列表（BUYER/SELLER/ADMIN/AUDITOR/INSPECTOR/CS）</li>
 *   <li>{@code phone}  : 手机号（脱敏后可传）</li>
 *   <li>{@code type}   : Token 类型: access / refresh</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Getter
@Component
@ConfigurationProperties(prefix = "yingjianxia.jwt")
public class JwtUtils {

    /* ========== 可配置参数 ========== */
    /**
     * 签名密钥（生产环境必须 256 位以上，配置在 Nacos 中）
     */
    private String secret = "yingjianxia-default-secret-key-please-change-this-in-production-env-2026";

    /**
     * Access Token 有效期，单位：毫秒，默认 2 小时
     */
    private long accessExpire = 2 * 60 * 60 * 1000L;

    /**
     * Refresh Token 有效期，单位：毫秒，默认 7 天
     */
    private long refreshExpire = 7 * 24 * 60 * 60 * 1000L;

    /**
     * Token 签发者
     */
    private String issuer = "yingjianxia-platform";

    /* ========== Setter 供 @ConfigurationProperties 使用 ========== */
    public void setSecret(String secret) { this.secret = secret; }
    public void setAccessExpire(long accessExpire) { this.accessExpire = accessExpire; }
    public void setRefreshExpire(long refreshExpire) { this.refreshExpire = refreshExpire; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    /* ========== 内部工具 ========== */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /* ========== 生成 Token ========== */

    /**
     * 生成 Access Token
     */
    public String generateAccessToken(Long userId, List<String> roles, String phone) {
        return doGenerate(userId, roles, phone, "access", accessExpire);
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(Long userId) {
        return doGenerate(userId, Collections.emptyList(), null, "refresh", refreshExpire);
    }

    private String doGenerate(Long userId, List<String> roles, String phone, String type, long expire) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expire);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .issuedAt(now)
                .expiration(exp)
                .id(UUID.randomUUID().toString().replace("-", ""))
                .claim("roles", roles == null ? Collections.emptyList() : roles)
                .claim("phone", phone != null ? phone : "")
                .claim("type", type)
                .signWith(getSigningKey())
                .compact();
    }

    /* ========== 解析 Token ========== */

    /**
     * 解析 Token（完整验签 + 过期校验），解析失败抛 JwtException
     *
     * @throws JwtException 任何不合法情况
     */
    public Claims parseToken(String token) {
        if (StrUtil.isBlank(token)) {
            throw new MalformedJwtException("Token 不能为空");
        }
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 安全解析（不抛异常，失败返回 empty）
     */
    public Optional<Claims> parseTokenSafe(String token) {
        try {
            return Optional.ofNullable(parseToken(token));
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT 签名无效: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT 格式错误: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT 不支持: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 参数错误: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /* ========== 便捷读取 ========== */

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    public Long getUserIdSafe(String token) {
        return parseTokenSafe(token)
                .map(c -> Long.valueOf(c.getSubject()))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = parseToken(token);
        return claims.get("roles", List.class) != null
                ? (List<String>) claims.get("roles", List.class)
                : Collections.emptyList();
    }

    public String getTokenType(String token) {
        return parseTokenSafe(token).map(c -> c.get("type", String.class)).orElse(null);
    }

    /**
     * 取剩余有效时间（毫秒），已过期返回 0
     */
    public long getRemainingExpireMs(String token) {
        return parseTokenSafe(token)
                .map(c -> Math.max(0L, c.getExpiration().getTime() - System.currentTimeMillis()))
                .orElse(0L);
    }

    /**
     * 判断是否过期
     */
    public boolean isExpired(String token) {
        return parseTokenSafe(token)
                .map(c -> c.getExpiration().before(new Date()))
                .orElse(true);
    }

    /**
     * 判断是否 Access Token
     */
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    /**
     * 判断是否 Refresh Token
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }
}
