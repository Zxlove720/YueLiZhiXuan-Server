package a311.college.auth;

import a311.college.thread.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * JWT 认证拦截器
 * <p>
 * 对所有请求生效（order 最小，最先执行）：
 * <ul>
 *     <li>携带合法访问令牌（JWT 签名有效、未过期、未进黑名单）时，将用户 id 与令牌写入 ThreadLocal；</li>
 *     <li>令牌缺失或不合法时不在此拦截器抛错，而是放行，由 {@code LoginInterceptor}
 *     根据 ThreadLocal 是否为空对受保护路径返回 401，保证公开接口（登录、注册、搜索等）不受影响；</li>
 *     <li>请求结束后清理 ThreadLocal，防止线程复用导致身份串用。</li>
 * </ul>
 * </p>
 */
@Slf4j
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    private final TokenService tokenService;

    public JwtAuthenticationInterceptor(JwtProperties jwtProperties, TokenService tokenService) {
        this.jwtProperties = jwtProperties;
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 1.提取令牌
        String accessToken = resolveAccessToken(request);
        if (accessToken == null) {
            // 未携带令牌，放行至后续拦截器（受保护路径将由 LoginInterceptor 返回 401）
            return true;
        }

        // 2.校验令牌（签名、有效期、黑名单），校验通过则写入 ThreadLocal
        Optional<Long> userIdOptional = tokenService.resolveUserId(accessToken);
        if (userIdOptional.isPresent()) {
            ThreadLocalUtil.setCurrentId(userIdOptional.get());
            ThreadLocalUtil.setCurrentToken(accessToken);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，释放资源，避免线程池复用导致的数据串用
        ThreadLocalUtil.clear();
    }

    /**
     * 从请求头提取访问令牌
     *
     * @param request 请求
     * @return 访问令牌；未携带时返回 null
     */
    private String resolveAccessToken(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        if (header == null || header.isBlank()) {
            return null;
        }
        String prefix = jwtProperties.getTokenPrefix();
        if (!header.startsWith(prefix)) {
            return null;
        }
        return header.substring(prefix.length()).trim();
    }

}
