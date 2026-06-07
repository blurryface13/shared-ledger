package com.spvermicelli.tripledger.identity.application.auth;

import com.spvermicelli.tripledger.identity.application.auth.command.BindWechatMobileCommand;
import com.spvermicelli.tripledger.identity.application.auth.command.RefreshAccessTokenCommand;
import com.spvermicelli.tripledger.identity.application.auth.command.WechatLoginCommand;
import com.spvermicelli.tripledger.identity.application.auth.result.BindWechatMobileResult;
import com.spvermicelli.tripledger.identity.application.auth.result.RefreshAccessTokenResult;
import com.spvermicelli.tripledger.identity.application.auth.result.WechatLoginResult;
import com.spvermicelli.tripledger.identity.domain.auth.model.RefreshTokenSession;
import com.spvermicelli.tripledger.identity.domain.auth.model.WechatIdentity;
import com.spvermicelli.tripledger.identity.domain.auth.repository.RefreshTokenSessionRepository;
import com.spvermicelli.tripledger.identity.domain.auth.service.WechatIdentityGateway;
import com.spvermicelli.tripledger.identity.domain.auth.valueobject.RefreshTokenStatus;
import com.spvermicelli.tripledger.identity.domain.user.model.User;
import com.spvermicelli.tripledger.identity.domain.user.repository.UserRepository;
import com.spvermicelli.tripledger.identity.domain.user.service.WechatMobileGateway;
import com.spvermicelli.tripledger.identity.infrastructure.security.AuthProperties;
import com.spvermicelli.tripledger.identity.infrastructure.security.JwtTokenService;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.domain.enums.UserStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final WechatIdentityGateway wechatIdentityGateway;
    private final WechatMobileGateway wechatMobileGateway;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final AuthProperties authProperties;

    public AuthApplicationService(
        UserRepository userRepository,
        WechatIdentityGateway wechatIdentityGateway,
        WechatMobileGateway wechatMobileGateway,
        JwtTokenService jwtTokenService,
        RefreshTokenSessionRepository refreshTokenSessionRepository,
        AuthProperties authProperties
    ) {
        this.userRepository = userRepository;
        this.wechatIdentityGateway = wechatIdentityGateway;
        this.wechatMobileGateway = wechatMobileGateway;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.authProperties = authProperties;
    }

    /**
     * 微信登录主流程。
     * 1. 后端携带小程序 code 向微信服务端换取 openid/unionid。
     * 2. 根据 openid 查找现有用户。
     * 2. 不存在则创建平台用户，但此时不直接签发正式访问令牌。
     * 3. 已绑定手机号的老用户直接返回 access token + refresh token。
     * 4. 未绑定手机号的新用户或历史遗留用户返回 bind token，驱动前端进入手机号绑定流程。
     */
    @Transactional
    public WechatLoginResult wechatLogin(WechatLoginCommand command) {
        // 根据code返回一个open id
        WechatIdentity wechatIdentity = wechatIdentityGateway.resolveMiniProgramIdentity(command.getCode());
        // 根据open id查找一条数据库的数据
        Optional<User> existingUser = userRepository.findByWechatOpenId(wechatIdentity.openId());

        // 判断返回里是否有值，如果有
        if (existingUser.isPresent()) {
            User resolvedUser = resolveLoginUser(existingUser.get(), wechatIdentity, command);
            if (resolvedUser == null) {
                return buildCancelledUserLoginResult();
            }
            return finishLogin(resolvedUser);
        }
        // 如果没有
        return finishLogin(createUser(wechatIdentity));
    }

    /**
     * 绑定微信手机号，并在绑定成功后签发正式访问令牌。
     */
    @Transactional
    public BindWechatMobileResult bindWechatMobile(BindWechatMobileCommand command) {
        if (command.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
        User existingUser = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        validateUserStatus(existingUser);
        validateBindMobileInput(command.getPhoneCode(), command.getManualMobile());
        String mobile = wechatMobileGateway.resolveMobile(command.getPhoneCode(), command.getManualMobile());
        String nickname = resolveBindNickname(command.getNickname(), existingUser.getNickname());
        String avatarUrl = resolveBindAvatarUrl(command.getAvatarUrl(), existingUser.getAvatarUrl());
        validateBindProfileInput(nickname);

        userRepository.findByMobile(mobile)
            .filter(user -> !Objects.equals(user.getId(), existingUser.getId()))
            .ifPresent(user -> {
                throw new BusinessException(ErrorCode.CONFLICT, "该手机号已被其他账号绑定");
            });

        User updatedUser = existingUser.updateProfile(nickname, avatarUrl, mobile);
        User savedUser = userRepository.save(updatedUser);

        TokenBundle tokenBundle = issueTokenBundle(savedUser.getId());
        return BindWechatMobileResult.builder()
            .userId(savedUser.getId())
            .nickname(savedUser.getNickname())
            .avatarUrl(savedUser.getAvatarUrl())
            .mobile(savedUser.getMobile())
            .mobileMasked(maskMobile(savedUser.getMobile()))
            .mobileBound(true)
            .token(tokenBundle.accessToken())
            .refreshToken(tokenBundle.refreshToken())
            .tokenExpireAt(tokenBundle.accessTokenExpireAt())
            .refreshTokenExpireAt(tokenBundle.refreshTokenExpireAt())
            .build();
    }

    /**
     * refresh token 采用“旋转续签”模式：
     * 每次续签都会把旧 refresh token 标记为 USED，并签发一枚新的 refresh token。
     * 这样可以在 refresh token 泄漏后最大化缩小风险窗口。
     */
    @Transactional
    public RefreshAccessTokenResult refreshAccessToken(RefreshAccessTokenCommand command) {
        String refreshTokenHash = sha256(command.getRefreshToken());
        RefreshTokenSession existingSession = refreshTokenSessionRepository.findByTokenHash(refreshTokenHash)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token 无效"));

        if (existingSession.getStatus() != RefreshTokenStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token 已失效");
        }
        if (existingSession.getExpireAt().isBefore(Instant.now())) {
            RefreshTokenSession expiredSession = existingSession.markExpired();
            refreshTokenSessionRepository.save(expiredSession);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token 已过期");
        }

        User user = userRepository.findById(existingSession.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "当前用户状态不允许登录");
        }

        String nextRefreshToken = buildRefreshToken();
        String nextRefreshTokenHash = sha256(nextRefreshToken);
        RefreshTokenSession usedSession = existingSession.markUsed(nextRefreshTokenHash);
        refreshTokenSessionRepository.save(usedSession);

        TokenBundle tokenBundle = issueTokenBundle(user.getId(), nextRefreshToken, nextRefreshTokenHash);
        return RefreshAccessTokenResult.builder()
            .token(tokenBundle.accessToken())
            .refreshToken(tokenBundle.refreshToken())
            .tokenExpireAt(tokenBundle.accessTokenExpireAt())
            .refreshTokenExpireAt(tokenBundle.refreshTokenExpireAt())
            .build();
    }

    /**
     * 安全同步用户信息
     * @param existingUser: 数据库中已存在的用户
     * @param wechatIdentity: 微信身份，包含open id和union id
     * @return 更新后的用户
     */
    private User updateExistingUser(User existingUser, WechatIdentity wechatIdentity) {
        return existingUser.refreshWechatIdentity(resolveUnionId(existingUser.getWechatUnionId(), wechatIdentity.unionId()))
            .updateProfile(resolveNickname(existingUser.getNickname(), existingUser.getWechatOpenId()), defaultAvatarUrl(existingUser.getAvatarUrl()), null);
    }

    /**
     * 用户状态（Status）处理
     * @param existingUser
     * @param wechatIdentity
     * @param command
     * @return 用户实体类
     */
    private User resolveLoginUser(User existingUser, WechatIdentity wechatIdentity, WechatLoginCommand command) {
        // 确认用户是否是注销状态
        if (existingUser.getStatus() == UserStatus.CANCELLED) {
            // 如果已注销，且ConfirmReRegister不是true
            if (!Boolean.TRUE.equals(command.getConfirmReRegister())) {
                // 返回 null
                return null;
            }
            // 将用户重新设置为激活状态
            return updateExistingUser(existingUser, wechatIdentity).reactivate();
        }
        // 用户本来就是正常状态，直接返回
        return updateExistingUser(existingUser, wechatIdentity);
    }

    private User createUser(WechatIdentity wechatIdentity) {
        return User.builder()
            .wechatOpenId(wechatIdentity.openId())
            .wechatUnionId(wechatIdentity.unionId())
            .nickname(resolveNickname(null, wechatIdentity.openId()))
            .avatarUrl("")
            .status(UserStatus.ACTIVE)
            .build();
    }

    private WechatLoginResult buildCancelledUserLoginResult() {
        return WechatLoginResult.builder()
            .mobileBound(false)
            .needBindMobile(false)
            .cancelledUser(true)
            .needConfirmReRegister(true)
            .build();
    }

    private WechatLoginResult finishLogin(User user) {
        User savedUser = userRepository.save(user);
        validateUserStatus(savedUser);

        if (!StringUtils.hasText(savedUser.getMobile())) {
            JwtTokenService.JwtIssuedToken bindToken = jwtTokenService.generateBindToken(savedUser.getId());
            return WechatLoginResult.builder()
                .bindToken(bindToken.token())
                .userId(savedUser.getId())
                .nickname(savedUser.getNickname())
                .avatarUrl(savedUser.getAvatarUrl())
                .mobileBound(false)
                .needBindMobile(true)
                .cancelledUser(false)
                .needConfirmReRegister(false)
                .build();
        }

        TokenBundle tokenBundle = issueTokenBundle(savedUser.getId());
        return WechatLoginResult.builder()
            .token(tokenBundle.accessToken())
            .refreshToken(tokenBundle.refreshToken())
            .userId(savedUser.getId())
            .nickname(savedUser.getNickname())
            .avatarUrl(savedUser.getAvatarUrl())
            .mobileBound(true)
            .needBindMobile(false)
            .cancelledUser(false)
            .needConfirmReRegister(false)
            .tokenExpireAt(tokenBundle.accessTokenExpireAt())
            .refreshTokenExpireAt(tokenBundle.refreshTokenExpireAt())
            .build();
    }

    /**
     * 辅助函数，验证用户状态，只有用户状态为ACTIVE猜允许登陆
     * @param user: 用户类
     */
    private void validateUserStatus(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "当前用户状态不允许登录");
        }
    }

    private String resolveNickname(String fallbackNickname, String wechatOpenId) {
        if (StringUtils.hasText(fallbackNickname)) {
            return fallbackNickname;
        }
        return "用户" + wechatOpenId.substring(Math.max(0, wechatOpenId.length() - 6));
    }

    private String defaultAvatarUrl(String avatarUrl) {
        return StringUtils.hasText(avatarUrl) ? avatarUrl : "";
    }

    private String resolveUnionId(String currentUnionId, String latestUnionId) {
        if (StringUtils.hasText(latestUnionId)) {
            return latestUnionId;
        }
        return currentUnionId;
    }

    private void validateBindMobileInput(String phoneCode, String manualMobile) {
        boolean hasPhoneCode = StringUtils.hasText(phoneCode);
        boolean hasManualMobile = StringUtils.hasText(manualMobile);
        if (!hasPhoneCode && !hasManualMobile) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "phoneCode 和 manualMobile 不能同时为空");
        }
        if (hasPhoneCode && hasManualMobile) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "phoneCode 和 manualMobile 不能同时传递");
        }
    }

    private String resolveBindNickname(String requestedNickname, String existingNickname) {
        String normalizedRequested = normalizeOptionalText(requestedNickname);
        if (StringUtils.hasText(normalizedRequested)) {
            return normalizedRequested;
        }
        return normalizeOptionalText(existingNickname);
    }

    private String resolveBindAvatarUrl(String requestedAvatarUrl, String existingAvatarUrl) {
        String normalizedRequested = normalizeOptionalText(requestedAvatarUrl);
        if (StringUtils.hasText(normalizedRequested)) {
            return normalizedRequested;
        }
        return normalizeOptionalText(existingAvatarUrl);
    }

    private void validateBindProfileInput(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "请先完善用户名");
        }
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    private TokenBundle issueTokenBundle(Long userId) {
        String refreshToken = buildRefreshToken();
        return issueTokenBundle(userId, refreshToken, sha256(refreshToken));
    }

    private TokenBundle issueTokenBundle(Long userId, String refreshToken, String refreshTokenHash) {
        JwtTokenService.JwtIssuedToken accessToken = jwtTokenService.generateAccessToken(userId);
        Instant refreshTokenExpireAt = Instant.now().plusSeconds(authProperties.getRefreshTokenExpireSeconds());

        RefreshTokenSession session = RefreshTokenSession.builder()
            .userId(userId)
            .tokenHash(refreshTokenHash)
            .status(RefreshTokenStatus.ACTIVE)
            .expireAt(refreshTokenExpireAt)
            .build();
        refreshTokenSessionRepository.save(session);

        return new TokenBundle(
            accessToken.token(),
            accessToken.expireAt(),
            refreshToken,
            refreshTokenExpireAt
        );
    }

    private String buildRefreshToken() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    private String sha256(String content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统不支持 SHA-256 摘要算法");
        }
    }

    private record TokenBundle(
        String accessToken,
        Instant accessTokenExpireAt,
        String refreshToken,
        Instant refreshTokenExpireAt
    ) {
    }
}
