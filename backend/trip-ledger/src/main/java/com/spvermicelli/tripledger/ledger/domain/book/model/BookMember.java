package com.spvermicelli.tripledger.ledger.domain.book.model;

import com.spvermicelli.tripledger.shared.domain.enums.MemberRole;
import com.spvermicelli.tripledger.shared.domain.enums.MemberStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookMember {
    private Long id;
    private Long bookId;
    private Long userId;
    private Long budgetAmountCent;
    private MemberRole memberRole;
    private MemberStatus memberStatus;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Long invitedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return memberStatus == MemberStatus.ACTIVE;
    }

    public boolean isOwner() {
        return memberRole == MemberRole.OWNER;
    }

    public boolean isAdmin() {
        return memberRole == MemberRole.ADMIN;
    }

    public boolean canManageMembers() {
        return isOwner() || isAdmin();
    }

    /**
     * 成员主动退出账本。
     * 退出只修改成员状态和离开时间，不删除历史关系。
     */
    public BookMember quit(LocalDateTime leftAt) {
        return rebuild(memberRole, MemberStatus.QUIT, joinedAt, leftAt, invitedByUserId);
    }

    /**
     * 成员被管理者移出账本。
     * V1 采用历史关系保留策略，因此这里仍然保留原成员记录。
     */
    public BookMember remove(LocalDateTime leftAt) {
        return rebuild(memberRole, MemberStatus.REMOVED, joinedAt, leftAt, invitedByUserId);
    }

    /**
     * 修改成员角色。
     * 该行为只负责角色切换，权限判断由应用服务在进入领域前完成。
     */
    public BookMember changeRole(MemberRole targetRole) {
        return rebuild(targetRole, memberStatus, joinedAt, leftAt, invitedByUserId);
    }

    /**
     * 历史成员重新加入账本时复用原记录。
     * 重新加入后统一恢复为普通成员，并刷新最近一次加入时间。
     */
    public BookMember reactivate(Long latestInvitedByUserId, LocalDateTime latestJoinedAt) {
        return rebuild(MemberRole.MEMBER, MemberStatus.ACTIVE, latestJoinedAt, null, latestInvitedByUserId);
    }

    public BookMember updateBudgetAmountCent(Long nextBudgetAmountCent) {
        Long normalized = nextBudgetAmountCent == null ? 0L : Math.max(nextBudgetAmountCent, 0L);
        return BookMember.builder()
            .id(id)
            .bookId(bookId)
            .userId(userId)
            .budgetAmountCent(normalized)
            .memberRole(memberRole)
            .memberStatus(memberStatus)
            .joinedAt(joinedAt)
            .leftAt(leftAt)
            .invitedByUserId(invitedByUserId)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    private BookMember rebuild(
        MemberRole targetRole,
        MemberStatus targetStatus,
        LocalDateTime targetJoinedAt,
        LocalDateTime targetLeftAt,
        Long targetInvitedByUserId
    ) {
        return BookMember.builder()
            .id(id)
            .bookId(bookId)
            .userId(userId)
            .budgetAmountCent(budgetAmountCent)
            .memberRole(targetRole)
            .memberStatus(targetStatus)
            .joinedAt(targetJoinedAt)
            .leftAt(targetLeftAt)
            .invitedByUserId(targetInvitedByUserId)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }
}
