package a311.college.auth;

/**
 * 双令牌对
 *
 * @param accessToken  访问令牌（JWT，无状态、有效期短）
 * @param refreshToken 刷新令牌（随机串，存 Redis、有效期长、可吊销）
 * @param userId       令牌所属用户 id
 */
public record TokenPair(String accessToken, String refreshToken, Long userId) {
}
