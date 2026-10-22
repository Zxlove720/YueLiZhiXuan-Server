package a311.college.auth;

import a311.college.exception.TokenInvalidException;
import a311.college.jwt.JWTUtils;
import cn.hutool.core.util.RandomUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 令牌服务：负责双令牌（JWT 访问令牌 + Redis 刷新会话）的生成、校验与吊销
 * <p>
 * 方案要点：
 * <ul>
 *     <li>访问令牌：JWT（HS256），无状态、可本地快速校验，有效期短（30 分钟）；
 *     每个访问令牌携带唯一 tokenId，与刷新会话一一对应；</li>
 *     <li>刷新会话：随机串存 Redis HASH（7 天滑动过期），<b>同一用户支持多个会话并存</b>
 *     （多端登录互不干扰），每次刷新轮换本会话的 tokenId 与随机串；</li>
 *     <li>黑名单：登出 / 改密 / 注销时把仍在有效期内的 JWT 的 tokenId 加入 Redis 黑名单，
 *     黑名单 TTL = 令牌剩余有效时间，到期自动清理；</li>
 *     <li>双重过期控制：每个会话独立记录「绝对过期起点」（30 天，不随刷新顺延）与
 *     「滑动过期截止」（7 天，每次刷新重置），保证长期活跃的会话也必须周期性重新认证。</li>
 * </ul>
 * 吊销语义：
 * <ul>
 *     <li>登出：仅吊销当前设备会话（revokeSession）；</li>
 *     <li>改密 / 注销：吊销该用户全部会话（revokeAllSessions）。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class TokenService {

    // JWT 自定义声明键
    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_TOKEN_ID = "tokenId";

    private final JwtProperties jwtProperties;

    private final TokenStore tokenStore;

    public TokenService(JwtProperties jwtProperties, TokenStore tokenStore) {
        this.jwtProperties = jwtProperties;
        this.tokenStore = tokenStore;
    }

    /**
     * 开启新会话并签发双令牌（登录 / 注册 / 改密成功时调用）
     *
     * @param userId 用户 id
     * @return 令牌对
     */
    public TokenPair startSession(Long userId) {
        return issueTokenPair(userId, System.currentTimeMillis() / 1000);
    }

    /**
     * 校验访问令牌并解析用户 id
     * <p>
     * 校验步骤：签名与有效期（JWT 自身）→ 黑名单（Redis 主动吊销）。
     * 校验失败统一返回空 Optional，由调用方决定如何响应。
     * </p>
     *
     * @param accessToken 访问令牌
     * @return 用户 id；令牌不合法时为空
     */
    public Optional<Long> resolveUserId(String accessToken) {
        try {
            Claims claims = JWTUtils.parseJWT(jwtProperties.getSecret(), accessToken);
            String tokenId = claims.get(CLAIM_TOKEN_ID, String.class);
            if (tokenId != null && tokenStore.isBlacklisted(tokenId)) {
                log.info("访问令牌已被吊销，tokenId：{}", tokenId);
                return Optional.empty();
            }
            Object userIdObj = claims.get(CLAIM_USER_ID);
            if (userIdObj == null) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(String.valueOf(userIdObj)));
        } catch (Exception e) {
            // 签名错误、令牌被篡改、已过期等，统一视为无效令牌
            log.info("访问令牌校验失败：{}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 刷新当前会话的令牌（刷新令牌轮换，仅影响发起刷新的这一端）
     * <p>
     * 校验顺序：会话存在 → 未超过滑动过期 → 未超过绝对过期 → 随机串匹配。
     * 全部通过后删除旧会话字段并签发新双令牌（绝对过期起点保持不变）。
     * </p>
     *
     * @param refreshToken 刷新令牌，格式 "userId:tokenId:secret"
     * @return 新的令牌对
     * @throws TokenInvalidException 会话超过绝对有效期，或刷新令牌不存在、已过期、不匹配
     */
    public TokenPair refreshTokens(String refreshToken) {
        RefreshTokenParts parts = parseRefreshToken(refreshToken);
        RefreshSession session = tokenStore.getRefreshSession(parts.userId(), parts.tokenId())
                .orElseThrow(() -> {
                    log.info("刷新令牌校验失败，会话不存在，userId：{}", parts.userId());
                    return new TokenInvalidException("刷新令牌无效或已过期，请重新登录");
                });
        long now = System.currentTimeMillis() / 1000;
        if (now >= session.getExpiryEpoch()) {
            tokenStore.deleteRefreshSession(parts.userId(), parts.tokenId());
            throw new TokenInvalidException("刷新令牌无效或已过期，请重新登录");
        }
        if (now - session.getStartEpoch() >= jwtProperties.getAbsoluteTtl()) {
            tokenStore.deleteRefreshSession(parts.userId(), parts.tokenId());
            log.info("会话已超过绝对有效期，要求重新登录，userId：{}", parts.userId());
            throw new TokenInvalidException("会话已到期，请重新登录");
        }
        if (!session.getSecret().equals(parts.secret())) {
            log.info("刷新令牌随机串不匹配，userId：{}", parts.userId());
            throw new TokenInvalidException("刷新令牌无效或已过期，请重新登录");
        }
        // 轮换：删除旧会话字段，按原绝对过期起点签发新双令牌
        tokenStore.deleteRefreshSession(parts.userId(), parts.tokenId());
        return issueTokenPair(parts.userId(), session.getStartEpoch());
    }

    /**
     * 吊销单个会话（登出：仅当前设备）
     * <p>
     * 通过当前访问令牌中的 tokenId 定位对应的刷新会话并删除；
     * 若访问令牌仍在有效期内，则同时加入黑名单使其立即失效。
     * </p>
     *
     * @param userId      用户 id
     * @param accessToken 当前访问令牌（可为 null）
     */
    public void revokeSession(Long userId, String accessToken) {
        if (userId != null) {
            parseTokenId(accessToken).ifPresent(tokenId ->
                    tokenStore.deleteRefreshSession(userId, tokenId));
        }
        blacklistAccessToken(accessToken);
    }

    /**
     * 吊销该用户全部会话（改密、注销：所有设备强制下线）
     *
     * @param userId      用户 id
     * @param accessToken 当前访问令牌（可为 null，用于黑名单当前 JWT）
     */
    public void revokeAllSessions(Long userId, String accessToken) {
        if (userId != null) {
            tokenStore.deleteAllRefreshSessions(userId);
        }
        blacklistAccessToken(accessToken);
    }

    /**
     * 签发双令牌（生成新的 tokenId 与随机串，写入新的刷新会话）
     * <p>
     * 刷新令牌格式为 "userId:tokenId:secret"，三个部分分别用于定位用户、
     * 定位 Redis 会话字段、校验持有者。
     * </p>
     *
     * @param userId     用户 id
     * @param startEpoch 会话绝对过期起点（秒级时间戳）
     * @return 令牌对
     */
    private TokenPair issueTokenPair(Long userId, long startEpoch) {
        String tokenId = RandomUtil.randomString(32);
        String secret = RandomUtil.randomString(32);
        long expiry = System.currentTimeMillis() / 1000 + jwtProperties.getRefreshTtl();
        tokenStore.saveRefreshSession(userId, tokenId,
                new RefreshSession(startEpoch, expiry, secret), jwtProperties.getRefreshTtl());
        String refreshToken = userId + ":" + tokenId + ":" + secret;
        String accessToken = createAccessToken(userId, tokenId);
        return new TokenPair(accessToken, refreshToken, userId);
    }

    /**
     * 解析访问令牌中的 tokenId 并将其加入黑名单
     *
     * @param accessToken 访问令牌
     */
    private void blacklistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            Claims claims = JWTUtils.parseJWT(jwtProperties.getSecret(), accessToken);
            String tokenId = claims.get(CLAIM_TOKEN_ID, String.class);
            Date expiration = claims.getExpiration();
            if (tokenId == null || expiration == null) {
                return;
            }
            long remainSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            tokenStore.addToBlacklist(tokenId, remainSeconds);
        } catch (Exception e) {
            // 解析失败说明令牌本身已不可用，无需加入黑名单
            log.info("访问令牌解析失败，跳过黑名单处理：{}", e.getMessage());
        }
    }

    /**
     * 从访问令牌中解析 tokenId（登出时定位需要吊销的刷新会话）
     *
     * @param accessToken 访问令牌
     * @return tokenId；解析失败返回空
     */
    private Optional<String> parseTokenId(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = JWTUtils.parseJWT(jwtProperties.getSecret(), accessToken);
            return Optional.ofNullable(claims.get(CLAIM_TOKEN_ID, String.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 生成访问令牌
     *
     * @param userId  用户 id
     * @param tokenId 会话唯一标识（同时写入 JWT 声明与 Redis 会话字段）
     * @return JWT 访问令牌
     */
    private String createAccessToken(Long userId, String tokenId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TOKEN_ID, tokenId);
        return JWTUtils.createJWT(jwtProperties.getSecret(),
                jwtProperties.getAccessTtl() * 1000,
                claims);
    }

    /**
     * 解析刷新令牌 "userId:tokenId:secret"
     *
     * @param refreshToken 刷新令牌
     * @return 三个组成部分
     * @throws TokenInvalidException 格式不合法
     */
    private RefreshTokenParts parseRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new TokenInvalidException("刷新令牌不能为空");
        }
        String[] parts = refreshToken.split(":", 3);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new TokenInvalidException("刷新令牌格式不合法");
        }
        try {
            return new RefreshTokenParts(Long.valueOf(parts[0]), parts[1], parts[2]);
        } catch (NumberFormatException e) {
            throw new TokenInvalidException("刷新令牌格式不合法");
        }
    }

    /**
     * 刷新令牌的三个组成部分
     */
    private record RefreshTokenParts(Long userId, String tokenId, String secret) {
    }

}
