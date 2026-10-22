package a311.college.config;

import a311.college.auth.JwtAuthenticationInterceptor;
import a311.college.auth.JwtProperties;
import a311.college.auth.TokenService;
import a311.college.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LoginInterceptorConfig implements WebMvcConfigurer {

    private final JwtProperties jwtProperties;

    private final TokenService tokenService;

    public LoginInterceptorConfig(JwtProperties jwtProperties, TokenService tokenService) {
        this.jwtProperties = jwtProperties;
        this.tokenService = tokenService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器（对受保护路径生效，ThreadLocal 中无用户则返回 401）
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/users/login",         // 用户登录
                        "/users/refresh",       // 刷新令牌
                        "/upload",              // 用户上传头像
                        "/users/register",      // 用户注册
                        "/users/checkUsername", // 检查用户名是否可用
                        "/users/checkPhone",    // 检查手机号是否可用
                        "/users/editCode",      // 用户请求修改密码发送验证码
                        "/users/edit",          // 用户修改密码
                        "/schools/page",        // 分页查询学校
                        "/schools/search",      // 用户搜索
                        "/schools/searchList",  // 搜索提示
                        "/schools/hotSchool1",  // 获取本省热门本科学校
                        "/schools/hotSchool2",  // 获取本省热门专科学校
                        "/schools/hotSchool3",  // 获取外省热门本科学校
                        "/schools/hotSchool4",  // 获取外省热门专科学校
                        "/schools/hotSchool",   // 获取热门学校
                        "/schools/hotMajor1",   // 获取热门本科专业
                        "/schools/hotMajor2",   // 获取热门专科专业
                        "/schools/hotRank",     // 获取热门学校排行榜
                        "/schools/basic"        // 获取强基计划学校
                ).order(1);
        // 注册 JWT 认证拦截器（对所有请求生效，校验令牌并写入 ThreadLocal）
        registry.addInterceptor(new JwtAuthenticationInterceptor(jwtProperties, tokenService))
                .order(0);
    }

}
