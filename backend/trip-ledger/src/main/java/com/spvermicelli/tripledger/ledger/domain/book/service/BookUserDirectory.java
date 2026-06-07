package com.spvermicelli.tripledger.ledger.domain.book.service;

import com.spvermicelli.tripledger.ledger.domain.book.valueobject.BookUserSummary;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 账本上下文查询成员用户摘要的端口。
 * 该端口由基础设施层适配到 identity，上下文之间只通过约定好的摘要结构协作。
 */
public interface BookUserDirectory {

    Map<Long, BookUserSummary> getByUserIds(Collection<Long> userIds);

    /**
     * 查询单个仍然有效的平台用户摘要。
     * 成员邀请等高风险流程不应仅信任前端传来的 userId，必须在后端再次确认目标用户有效存在。
     */
    Optional<BookUserSummary> getActiveUser(Long userId);

    /**
     * 通过手机号模糊搜索有效用户，供共享账本邀请流程使用。
     */
    java.util.List<BookUserSummary> searchActiveUsersByPhoneKeyword(String keyword);

    /**
     * 通过昵称模糊搜索有效用户，供共享账本邀请流程使用。
     */
    java.util.List<BookUserSummary> searchActiveUsersByNicknameKeyword(String keyword);
}
