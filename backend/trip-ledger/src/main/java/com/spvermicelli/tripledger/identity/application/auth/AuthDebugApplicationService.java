package com.spvermicelli.tripledger.identity.application.auth;

import com.spvermicelli.tripledger.identity.application.auth.result.MockLoginUserResult;
import com.spvermicelli.tripledger.identity.domain.user.model.User;
import com.spvermicelli.tripledger.identity.domain.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthDebugApplicationService {

    private static final String MOCK_OPEN_ID_PREFIX = "mock-openid-";

    private final UserRepository userRepository;

    public AuthDebugApplicationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<MockLoginUserResult> listMockLoginUsers() {
        return userRepository.findByWechatOpenIdPrefix(MOCK_OPEN_ID_PREFIX)
            .stream()
            .map(this::toResult)
            .toList();
    }

    private MockLoginUserResult toResult(User user) {
        return MockLoginUserResult.builder()
            .userId(user.getId())
            .openId(user.getWechatOpenId())
            .suggestedCode(resolveSuggestedCode(user.getWechatOpenId()))
            .nickname(user.getNickname())
            .mobileBound(StringUtils.hasText(user.getMobile()))
            .mobileMasked(maskMobile(user.getMobile()))
            .status(user.getStatus() == null ? null : user.getStatus().name())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    private String resolveSuggestedCode(String openId) {
        if (!StringUtils.hasText(openId) || !openId.startsWith(MOCK_OPEN_ID_PREFIX)) {
            return openId;
        }
        return openId.substring(MOCK_OPEN_ID_PREFIX.length());
    }

    private String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}
