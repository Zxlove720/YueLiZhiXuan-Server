package a311.college.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 认证相关配置
 * <p>
 * 只引入 spring-security-crypto 提供 BCrypt 密码散列，
 * 不引入完整的 Spring Security 框架，保持项目现有自定义拦截器认证方案。
 * </p>
 */
@Configuration
public class AuthConfig {

    /**
     * BCrypt 密码散列器
     * <p>
     * 默认工作因子（cost = 10）：每次散列约 60~100ms，能在“暴力破解难度”
     * 与“登录体验”之间取得平衡；如机器性能充裕可调整为 12。
     * </p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
