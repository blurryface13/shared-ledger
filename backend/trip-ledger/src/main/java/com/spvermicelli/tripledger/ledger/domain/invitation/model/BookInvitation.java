package com.spvermicelli.tripledger.ledger.domain.invitation.model;

import com.spvermicelli.tripledger.shared.domain.enums.InvitationStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 账本邀请领域模型。
 * V1 邀请采用单条记录生命周期流转：PENDING -> ACCEPTED / REJECTED / EXPIRED。
 */
@Getter
@Builder
public class BookInvitation {
    private Long id;
    private Long bookId;
    private Long inviterUserId;
    private Long inviteeUserId;
    private InvitationStatus status;
    private LocalDateTime invitedAt;
    private LocalDateTime handledAt;
    private String remark;

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    /**
     * 邀请有效期当前统一定义为 24 小时。
     * 表结构中不单独存 expireAt，而是在领域侧通过 invitedAt 推导。
     */
    public LocalDateTime getExpireAt() {
        return invitedAt == null ? null : invitedAt.plusDays(1);
    }

    public boolean isExpired(LocalDateTime now) {
        return isPending() && getExpireAt() != null && !getExpireAt().isAfter(now);
    }

    public BookInvitation accept(LocalDateTime latestHandledAt) {
        return rebuild(InvitationStatus.ACCEPTED, latestHandledAt);
    }

    public BookInvitation reject(LocalDateTime latestHandledAt) {
        return rebuild(InvitationStatus.REJECTED, latestHandledAt);
    }

    public BookInvitation expire(LocalDateTime latestHandledAt) {
        return rebuild(InvitationStatus.EXPIRED, latestHandledAt);
    }

    public BookInvitation revoke(LocalDateTime latestHandledAt) {
        return rebuild(InvitationStatus.REVOKED, latestHandledAt);
    }

    private BookInvitation rebuild(InvitationStatus targetStatus, LocalDateTime latestHandledAt) {
        return BookInvitation.builder()
            .id(id)
            .bookId(bookId)
            .inviterUserId(inviterUserId)
            .inviteeUserId(inviteeUserId)
            .status(targetStatus)
            .invitedAt(invitedAt)
            .handledAt(latestHandledAt)
            .remark(remark)
            .build();
    }
}
