package a311.college.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 * <p>
 * 对应 application.yml 中 college.jwt.* 配置项。
 * HS256 签名要求 secret 至少 32 字节，生产环境务必通过环境变量 JWT_SECRET 注入。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "college.jwt")
public class JwtProperties {

    /**
     * 签名密钥（HS256，至少 32 字节）
     */
    private String secret;

    /**
     * 访问令牌有效期（秒），默认 30 分钟
     */
    private long accessTtl = 1800;

    /**
     * 刷新令牌有效期（秒），默认 7 天
     */
    private long refreshTtl = 604800;

    /**
     * 会话绝对有效期（秒），默认 30 天
     * <p>
     * 无论会话期间用户多活跃（刷新令牌如何顺延），
     * 一旦距离上次登录超过该时长，就必须重新登录。
     * </p>
     */
    private long absoluteTtl = 2592000;

    /**
     * 令牌携带的请求头名称
     */
    private String header = "Authorization";

    /**
     * 请求头中令牌的前缀
     */
    private String tokenPrefix = "Bearer ";

}
