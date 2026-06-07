package com.spvermicelli.tripledger.identity.infrastructure.security;

import com.spvermicelli.tripledger.identity.domain.auth.valueobject.AccessTokenType;
import com.spvermicelli.tripledger.identity.domain.user.repository.UserRepository;
import com.spvermicelli.tripledger.shared.common.context.LoginUser;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 基于 JWT 的接口鉴权拦截器。
 * 它承担三件事：
 * 1. 判断当前路径是否需要鉴权；
 * 2. 校验 token 类型是否符合接口用途；
 * 3. 确认 token 对应用户主体仍然有效，避免注销后旧 token 继续可用。
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final AuthProperties authProperties;
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public JwtAuthenticationInterceptor(
        AuthProperties authProperties,
        JwtTokenService jwtTokenService,
        UserRepository userRepository
    ) {
        this.authProperties = authProperties;
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        if (!authProperties.isEnabled()) {
            tryPopulateUserContext(request);
            return true;
        }

        if (isExcluded(request.getRequestURI())) {
            return true;
        }

        String token = resolveToken(request, true);
        LoginUser loginUser = jwtTokenService.parseToken(token);
        validateTokenType(request.getRequestURI(), loginUser.getTokenType());
        validateUserStatus(loginUser);
        UserContextHolder.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    private boolean isExcluded(String requestUri) {
        List<String> excludePaths = authProperties.getExcludePaths();
        for (String excludePath : excludePaths) {
            if (antPathMatcher.match(excludePath, requestUri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在 dev 等免鉴权环境中，如果前端主动携带了 token，仍然解析出用户上下文。
     * 这样像“绑定手机号”这类依赖当前用户身份的接口，在免鉴权模式下也能正常联调。
     */
    private void tryPopulateUserContext(HttpServletRequest request) {
        String token = resolveToken(request, false);
        if (!StringUtils.hasText(token)) {
            return;
        }
        LoginUser loginUser = jwtTokenService.parseToken(token);
        validateUserStatus(loginUser);
        UserContextHolder.set(loginUser);
    }

    /**
     * 解析token
     * @param request
     * @param required
     * @return
     */
    private String resolveToken(HttpServletRequest request, boolean required) {
        String authorization = request.getHeader(authProperties.getHeaderName());
        if (!StringUtils.hasText(authorization)) {
            if (required) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            return null;
        }

        String prefix = authProperties.getTokenPrefix() + " ";
        if (!authorization.startsWith(prefix)) {
            if (required) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authorization 头格式不正确");
            }
            return null;
        }
        return authorization.substring(prefix.length());
    }

    /**
     * 不同接口允许的 token 类型不同。
     * 1. 绑定手机号接口：允许 ACCESS 或 BIND_MOBILE。
     * 2. 其他受保护接口：仅允许 ACCESS。
     */
    private void validateTokenType(String requestUri, String tokenType) {
        if (isBindMobilePath(requestUri)) {
            if (AccessTokenType.ACCESS.name().equals(tokenType)
                || AccessTokenType.BIND_MOBILE.name().equals(tokenType)) {
                return;
            }
            throw new BusinessException(ErrorCode.TOKEN_TYPE_INVALID, "当前 token 不能用于绑定手机号");
        }

        if (!AccessTokenType.ACCESS.name().equals(tokenType)) {
            throw new BusinessException(ErrorCode.TOKEN_TYPE_INVALID, "当前 token 不能访问业务接口");
        }
    }

    private boolean isBindMobilePath(String requestUri) {
        for (String bindMobilePath : authProperties.getBindMobilePaths()) {
            if (antPathMatcher.match(bindMobilePath, requestUri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 访问受保护接口时，除了 token 本身合法，还需要确认用户主体仍然处于可用状态。
     * 这样用户注销后，旧 access token 会立即失效，不会留下 2 小时的权限空窗。
     */
    private void validateUserStatus(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
        boolean active = userRepository.findById(loginUser.getUserId())
            .map(user -> user.isActive())
            .orElse(false);
        if (!active) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号不可用，请重新登录");
        }
    }
}
