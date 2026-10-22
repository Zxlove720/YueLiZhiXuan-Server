package a311.college.constant.redis;

/**
 * 用户缓存键常量
 */
public class UserRedisKey {

    // 用户验证码
    // 用户修改密码验证码
    public static final String USER_EDIT_CODE_KEY = "college:user:code:edit:";
    // 用户注销验证码
    public static final String USER_DELETE_CODE_KEY = "college:user:code:delete:";
    // 验证码发送频控，key = 前缀 + phone：60 秒内只允许发送一条
    public static final String USER_CODE_LIMIT_KEY = "college:user:code:limit:";
    // 验证码连续校验失败计数，key = 前缀 + phone，达到上限后锁定
    public static final String USER_CODE_FAIL_KEY = "college:user:code:fail:";

    // 同一手机号两次发送验证码的最小间隔（秒）
    public static final Long USER_CODE_SEND_INTERVAL = 60L;
    // 验证码连续失败锁定阈值与锁定时长（秒）
    public static final Integer USER_CODE_FAIL_LIMIT = 5;
    public static final Long USER_CODE_FAIL_TTL = 10 * 60L;

    // 用户刷新令牌（refresh token），key = 前缀 + userId，value = 刷新令牌
    public static final String USER_REFRESH_KEY = "college:user:refresh:";

    // 访问令牌黑名单，key = 前缀 + JWT 的 tokenId，用于登出/改密后主动吊销仍在有效期内的 JWT
    public static final String USER_BLACKLIST_KEY = "college:user:blacklist:";

    // 会话起始标记，key = 前缀 + userId
    // 仅在登录/注册/改密时写入，且不随刷新顺延，是实现「绝对过期上限」的判定依据
    public static final String USER_SESSION_KEY = "college:user:session:";

    // 验证码过期时间（秒）
    public static final Long USER_CODE_TTL = 60L;

    // 访问令牌（access token）有效期（秒），与 JWT 的 exp 保持一致
    public static final Long ACCESS_TOKEN_TTL = 1800L;

    // 刷新令牌（refresh token）有效期（秒），7 天
    public static final Long REFRESH_TOKEN_TTL = 7 * 24 * 3600L;

    // 会话绝对有效期（秒），30 天：无论多活跃，超过该时长必须重新登录
    public static final Long SESSION_ABSOLUTE_TTL = 30 * 24 * 3600L;

    // 验证码有效期
    public static final String CODE_TIME_MESSAGE = "验证码30S内有效";

}
