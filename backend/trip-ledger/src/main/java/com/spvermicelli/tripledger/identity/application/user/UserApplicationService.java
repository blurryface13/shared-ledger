package com.spvermicelli.tripledger.identity.application.user;

import com.spvermicelli.tripledger.identity.domain.auth.repository.RefreshTokenSessionRepository;
import com.spvermicelli.tripledger.identity.application.user.command.CancelCurrentUserCommand;
import com.spvermicelli.tripledger.identity.application.user.command.UpdateCurrentUserCommand;
import com.spvermicelli.tripledger.identity.application.user.result.CurrentUserResult;
import com.spvermicelli.tripledger.identity.domain.user.model.User;
import com.spvermicelli.tripledger.identity.domain.user.repository.UserRepository;
import com.spvermicelli.tripledger.identity.domain.user.service.WechatMobileGateway;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 当前用户应用服务。
 * 该服务只处理“当前登录用户自己”的资料维护与注销用例。
 */
@Service
public class UserApplicationService {

    private final UserRepository userRepository;
    private final WechatMobileGateway wechatMobileGateway;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;

    public UserApplicationService(
        UserRepository userRepository,
        WechatMobileGateway wechatMobileGateway,
        RefreshTokenSessionRepository refreshTokenSessionRepository
    ) {
        this.userRepository = userRepository;
        this.wechatMobileGateway = wechatMobileGateway;
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
    }

    /**
     * 查询当前登录用户的资料。
     * 该用例只暴露“当前用户自己”可见的数据，不接受外部传入用户 id。
     */
    @Transactional(readOnly = true)
    public CurrentUserResult getCurrentUser(Long currentUserId) {
        User existingUser = loadCurrentUser(currentUserId);
        ensureActive(existingUser);
        return toResult(existingUser);
    }

    /**
     * 更新当前用户资料。
     * 接口不接受目标用户 id，因此天然只允许修改当前 token 对应用户自身。
     */
    @Transactional
    public CurrentUserResult updateCurrentUser(UpdateCurrentUserCommand command) {
        validateUpdatableFields(command);
        User existingUser = loadCurrentUser(command.getCurrentUserId());
        ensureActive(existingUser);

        String mobile = null;
        if (StringUtils.hasText(command.getPhoneCode()) || StringUtils.hasText(command.getManualMobile())) {
            mobile = wechatMobileGateway.resolveMobile(command.getPhoneCode(), command.getManualMobile());
            userRepository.findByMobile(mobile)
                .filter(user -> !Objects.equals(user.getId(), existingUser.getId()))
                .ifPresent(user -> {
                    throw new BusinessException(ErrorCode.CONFLICT, "该手机号已被其他账号绑定");
                });
        }

        User savedUser = userRepository.save(existingUser.updateProfile(command.getNickname(), command.getAvatarUrl(), mobile));
        return toResult(savedUser);
    }

    /**
     * 注销当前用户账号。
     * 仅将状态改为 CANCELLED，保留历史数据。
     */
    @Transactional
    public void cancelCurrentUser(CancelCurrentUserCommand command) {
        User existingUser = loadCurrentUser(command.getCurrentUserId());
        ensureActive(existingUser);
        userRepository.save(existingUser.cancel());
        refreshTokenSessionRepository.findActiveByUserId(existingUser.getId())
            .forEach(session -> refreshTokenSessionRepository.save(session.markRevoked()));
    }

    private void validateUpdatableFields(UpdateCurrentUserCommand command) {
        if (!StringUtils.hasText(command.getNickname())
            && !StringUtils.hasText(command.getAvatarUrl())
            && !StringUtils.hasText(command.getPhoneCode())
            && !StringUtils.hasText(command.getManualMobile())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "nickname、avatarUrl、manualMobile、phoneCode 不能同时为空");
        }
        if (StringUtils.hasText(command.getPhoneCode()) && StringUtils.hasText(command.getManualMobile())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "manualMobile 和 phoneCode 不能同时传递");
        }
    }

    private User loadCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
        return userRepository.findById(currentUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    private void ensureActive(User user) {
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "当前用户状态不允许执行该操作");
        }
    }

    private CurrentUserResult toResult(User user) {
        return CurrentUserResult.builder()
            .userId(user.getId())
            .nickname(user.getNickname())
            .avatarUrl(user.getAvatarUrl())
            .mobile(user.getMobile())
            .mobileMasked(maskMobile(user.getMobile()))
            .mobileBound(StringUtils.hasText(user.getMobile()))
            .status(user.getStatus().getCode())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    private String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}
