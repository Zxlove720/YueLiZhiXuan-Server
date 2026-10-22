package a311.college.thread;

import lombok.extern.slf4j.Slf4j;

/**
 * ThreadLocal 工具类
 * <p>
 * 用于在单个请求线程内传递用户身份（用户 id）与当前请求携带的访问令牌，
 * 由认证拦截器在 preHandle 中写入、afterCompletion 中清理，避免线程池场景下的数据串用。
 * </p>
 */
@Slf4j
public class ThreadLocalUtil {

    // 当前登录用户的 id
    private static final ThreadLocal<Long> USER_ID_THREAD_LOCAL = new ThreadLocal<>();

    // 当前请求携带的访问令牌（登出时用于加入黑名单）
    private static final ThreadLocal<String> ACCESS_TOKEN_THREAD_LOCAL = new ThreadLocal<>();

    // 设置当前线程的用户 id
    public static void setCurrentId(Long id) {
        USER_ID_THREAD_LOCAL.set(id);
    }

    // 获取当前线程的用户 id
    public static Long getCurrentId() {
        return USER_ID_THREAD_LOCAL.get();
    }

    // 设置当前线程的访问令牌
    public static void setCurrentToken(String token) {
        ACCESS_TOKEN_THREAD_LOCAL.set(token);
    }

    // 获取当前线程的访问令牌
    public static String getCurrentToken() {
        return ACCESS_TOKEN_THREAD_LOCAL.get();
    }

    // 清空当前线程的用户 id
    public static void removeCurrentId() {
        USER_ID_THREAD_LOCAL.remove();
    }

    // 清空当前线程保存的全部身份信息（用户 id 与访问令牌）
    public static void clear() {
        USER_ID_THREAD_LOCAL.remove();
        ACCESS_TOKEN_THREAD_LOCAL.remove();
    }

}
