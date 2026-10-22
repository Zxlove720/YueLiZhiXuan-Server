package a311.college.auth;

import java.util.Optional;

/**
 * 令牌存储抽象
 * <p>
 * 将令牌相关状态（刷新会话、访问令牌黑名单）的读写抽象为接口，
 * 生产环境使用 {@link RedisTokenStore} 实现；单元测试中可使用内存实现替换，
 * 避免测试依赖真实 Redis。
 * </p>
 */
public interface TokenStore {

    /**
     * 保存/更新一个会话的刷新数据（多会话：同一用户可有多个并存，field = tokenId）
     * <p>
     * 实现需顺带清理该用户下已过期的会话字段。
     * </p>
     *
     * @param userId     用户 id
     * @param tokenId    会话唯一标识（与访问令牌 JWT 中的 tokenId 一致）
     * @param session    会话数据（绝对过期起点、滑动过期截止点、随机串）
     * @param ttlSeconds 存储键的有效期（秒）
     */
    void saveRefreshSession(Long userId, String tokenId, RefreshSession session, long ttlSeconds);

    /**
     * 读取一个会话的刷新数据
     *
     * @param userId  用户 id
     * @param tokenId 会话唯一标识
     * @return 会话数据；不存在（过期/被吊销/被删除）返回空
     */
    Optional<RefreshSession> getRefreshSession(Long userId, String tokenId);

    /**
     * 删除一个会话（登出：仅吊销当前设备）
     *
     * @param userId  用户 id
     * @param tokenId 会话唯一标识
     */
    void deleteRefreshSession(Long userId, String tokenId);

    /**
     * 删除该用户全部会话（改密、注销时调用）
     *
     * @param userId 用户 id
     */
    void deleteAllRefreshSessions(Long userId);

    /**
     * 将访问令牌唯一标识加入黑名单（登出后 JWT 在剩余有效期内不可再用）
     *
     * @param tokenId    访问令牌唯一标识（JWT 中的 tokenId 声明）
     * @param ttlSeconds 黑名单有效期（秒），通常为令牌剩余有效时间
     */
    void addToBlacklist(String tokenId, long ttlSeconds);

    /**
     * 判断访问令牌是否在黑名单中
     *
     * @param tokenId 访问令牌唯一标识
     * @return true 表示已吊销
     */
    boolean isBlacklisted(String tokenId);

}
