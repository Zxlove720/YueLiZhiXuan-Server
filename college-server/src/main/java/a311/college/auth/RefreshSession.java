package a311.college.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 单个会话的刷新令牌数据（存于 Redis HASH，field = tokenId）
 * <p>
 * 一个用户可同时拥有多个会话（多端登录），每个会话独立记录
 * 绝对过期起点与滑动过期截止点。
 * </p>
 */
@Data
@AllArgsConstructor
public class RefreshSession {

    /** 会话起始时刻（秒级时间戳），绝对过期计时的起点，刷新时不顺延 */
    private long startEpoch;

    /** 刷新令牌过期时刻（秒级时间戳），每次刷新重置 */
    private long expiryEpoch;

    /** 刷新令牌随机串（校验持有者，防伪造） */
    private String secret;

}
