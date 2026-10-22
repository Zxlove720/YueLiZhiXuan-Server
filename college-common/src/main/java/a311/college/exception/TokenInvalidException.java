package a311.college.exception;

/**
 * 令牌无效异常
 * <p>
 * 访问令牌（JWT）签名不合法、已过期或已被加入黑名单，以及刷新令牌不存在、
 * 不匹配或已过期时抛出。
 * </p>
 */
public class TokenInvalidException extends BaseException {

    public TokenInvalidException() {
    }

    public TokenInvalidException(String msg) {
        super(msg);
    }

}
