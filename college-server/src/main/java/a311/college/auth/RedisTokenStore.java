package a311.college.auth;

import a311.college.constant.redis.UserRedisKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的令牌存储实现
 * <p>
 * 刷新会话使用 HASH 存储：key = college:user:refresh:{userId}，
 * field = tokenId，value = "start:expiry:secret"。
 * 保存时顺带清理已过期的会话字段，避免长期不活跃用户的残留数据。
 * </p>
 */
@Component
public class RedisTokenStore implements TokenStore {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTokenStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void saveRefreshSession(Long userId, String tokenId, RefreshSession session, long ttlSeconds) {
        String key = UserRedisKey.USER_REFRESH_KEY + userId;
        // 顺带清理该用户下已过期的会话字段（会话数量极少，遍历无压力）
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        long now = System.currentTimeMillis() / 1000;
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            RefreshSession old = decode(String.valueOf(entry.getValue()));
            if (old != null && old.getExpiryEpoch() <= now) {
                stringRedisTemplate.opsForHash().delete(key, entry.getKey());
            }
        }
        stringRedisTemplate.opsForHash().put(key, tokenId, encode(session));
        stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<RefreshSession> getRefreshSession(Long userId, String tokenId) {
        Object value = stringRedisTemplate.opsForHash()
                .get(UserRedisKey.USER_REFRESH_KEY + userId, tokenId);
        return Optional.ofNullable(decode(String.valueOf(value)));
    }

    @Override
    public void deleteRefreshSession(Long userId, String tokenId) {
        stringRedisTemplate.opsForHash()
                .delete(UserRedisKey.USER_REFRESH_KEY + userId, tokenId);
    }

    @Override
    public void deleteAllRefreshSessions(Long userId) {
        stringRedisTemplate.delete(UserRedisKey.USER_REFRESH_KEY + userId);
    }

    @Override
    public void addToBlacklist(String tokenId, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            // 令牌已过期，无需加入黑名单
            return;
        }
        stringRedisTemplate.opsForValue()
                .set(UserRedisKey.USER_BLACKLIST_KEY + tokenId, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(UserRedisKey.USER_BLACKLIST_KEY + tokenId));
    }

    private String encode(RefreshSession session) {
        return session.getStartEpoch() + ":" + session.getExpiryEpoch() + ":" + session.getSecret();
    }

    private RefreshSession decode(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        String[] parts = value.split(":", 3);
        if (parts.length != 3) {
            return null;
        }
        try {
            return new RefreshSession(Long.parseLong(parts[0]), Long.parseLong(parts[1]), parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
