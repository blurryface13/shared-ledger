package com.spvermicelli.tripledger.ledger.infrastructure.identity;

import com.spvermicelli.tripledger.identity.domain.user.repository.UserRepository;
import com.spvermicelli.tripledger.ledger.domain.book.service.BookUserDirectory;
import com.spvermicelli.tripledger.ledger.domain.book.valueobject.BookUserSummary;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 通过 identity 上下文的仓储，组装账本上下文所需的用户摘要。
 * 这里刻意不把 User 领域对象继续向上透传，避免上下文耦合扩散。
 */
@Component
public class IdentityBookUserDirectory implements BookUserDirectory {

    private final UserRepository userRepository;

    public IdentityBookUserDirectory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Map<Long, BookUserSummary> getByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findByIds(userIds)
            .stream()
            .collect(java.util.stream.Collectors.toMap(
                user -> user.getId(),
                this::toSummary,
                (left, right) -> left
            ));
    }

    @Override
    public Optional<BookUserSummary> getActiveUser(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId)
            .filter(user -> user.isActive())
            .map(this::toSummary);
    }

    @Override
    public List<BookUserSummary> searchActiveUsersByPhoneKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return userRepository.searchActiveByMobileKeyword(keyword.trim())
            .stream()
            .map(this::toSummary)
            .toList();
    }

    @Override
    public List<BookUserSummary> searchActiveUsersByNicknameKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return userRepository.searchActiveByNicknameKeyword(keyword.trim())
            .stream()
            .map(this::toSummary)
            .toList();
    }

    private BookUserSummary toSummary(com.spvermicelli.tripledger.identity.domain.user.model.User user) {
        return BookUserSummary.builder()
            .userId(user.getId())
            .nickname(user.getNickname())
            .avatarUrl(user.getAvatarUrl())
            .phoneNumber(maskMobile(user.getMobile()))
            .build();
    }

    private String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}
