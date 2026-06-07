package com.spvermicelli.tripledger.ledger.domain.book.valueobject;

import lombok.Builder;
import lombok.Getter;

/**
 * 账本上下文内部使用的用户摘要值对象。
 * 仅保留账本展示真正需要的最小字段，避免直接暴露 identity 领域完整用户模型。
 */
@Getter
@Builder
public class BookUserSummary {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String phoneNumber;
}
