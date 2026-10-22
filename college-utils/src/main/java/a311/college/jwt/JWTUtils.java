package a311.college.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 * <p>
 * 基于 jjwt 0.12.x 新 API 实现，使用 HS256 签名算法。
 * 注意：HS256 要求密钥长度至少 256 bit（32 字节），密钥过短会在创建时抛出
 * {@link io.jsonwebtoken.security.WeakKeyException}，便于在开发阶段尽早发现问题。
 * </p>
 */
public class JWTUtils {

    /**
     * 生成 JWT 令牌
     * <p>
     * 自定义业务声明直接放在 claims 中传递（建议使用自定义键名，避免与 JWT 保留声明冲突），
     * 过期时间由本工具统一计算（exp = 当前时间 + millis）。
     * </p>
     *
     * @param secretKey 签名密钥（HS256 要求至少 32 字节）
     * @param millis    令牌有效期（毫秒）
     * @param claims    自定义声明数据
     * @return JWT 令牌字符串
     */
    public static String createJWT(String secretKey, long millis, Map<String, Object> claims) {
        long expMillis = System.currentTimeMillis() + millis;
        Date expTime = new Date(expMillis);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(expTime)
                // 显式指定 HS256，避免 jjwt 依据密钥长度自动推断算法导致行为不一致
                .signWith(createSecretKey(secretKey), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析 JWT 令牌
     * <p>
     * 解析失败（签名不合法、令牌被篡改、已过期等）会抛出对应的
     * {@link io.jsonwebtoken.JwtException} 子类异常，由调用方统一处理。
     * </p>
     *
     * @param secretKey 签名密钥
     * @param token     JWT 令牌
     * @return Claims 声明数据
     */
    public static Claims parseJWT(String secretKey, String token) {
        return Jwts.parser()
                .verifyWith(createSecretKey(secretKey))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 根据密钥字符串构造 HS256 签名密钥
     *
     * @param secretKey 密钥字符串
     * @return SecretKey 签名密钥
     */
    private static SecretKey createSecretKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

}
