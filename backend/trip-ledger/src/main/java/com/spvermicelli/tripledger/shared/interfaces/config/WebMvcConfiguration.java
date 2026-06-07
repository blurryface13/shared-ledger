package com.spvermicelli.tripledger.shared.interfaces.config;

import com.spvermicelli.tripledger.identity.infrastructure.security.JwtAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 * 所有 `/api/**` 请求统一进入 JWT 拦截器，由业务认证规则决定是否放行。
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;
    private final CorsProperties corsProperties;
    private final FileStorageProperties fileStorageProperties;

    public WebMvcConfiguration(
        JwtAuthenticationInterceptor jwtAuthenticationInterceptor,
        CorsProperties corsProperties,
        FileStorageProperties fileStorageProperties
    ) {
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
        this.corsProperties = corsProperties;
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor).addPathPatterns("/api/**");
    }

    /**
     * 统一配置浏览器端的跨域白名单。
     * 这样本地前端联调、未来 Web 管理端以及正式域名访问时，都可以通过配置而不是改代码来控制来源范围。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(corsProperties.getPathPattern())
            .allowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(String[]::new))
            .allowedMethods(corsProperties.getAllowedMethods().toArray(String[]::new))
            .allowedHeaders(corsProperties.getAllowedHeaders().toArray(String[]::new))
            .exposedHeaders(corsProperties.getExposedHeaders().toArray(String[]::new))
            .allowCredentials(corsProperties.isAllowCredentials())
            .maxAge(corsProperties.getMaxAgeSeconds());
    }

    /**
     * 头像与账本封面采用公开静态资源映射，便于前端直接预览。
     * 账单图片不走静态映射，统一通过 `/api/v1/files/billing/**` 做鉴权访问。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String avatarLocation = fileStorageProperties.resolveAvatarDir().toUri().toString();
        String coverLocation = fileStorageProperties.resolveCoverDir().toUri().toString();
        registry.addResourceHandler("/avatar/**").addResourceLocations(avatarLocation);
        registry.addResourceHandler("/cover/**").addResourceLocations(coverLocation);
    }
}
