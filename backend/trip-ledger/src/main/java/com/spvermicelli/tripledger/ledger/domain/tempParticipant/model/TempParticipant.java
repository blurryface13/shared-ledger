package com.spvermicelli.tripledger.ledger.domain.tempParticipant.model;

import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantStatus;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 临时成员领域模型。
 * 临时成员用于承载“未直接注册为正式成员、但会出现在账单责任归属中”的对象。
 *
 * 当前设计约束：
 * 1. 临时成员一定归属于某个账本；
 * 2. 创建人与挂靠正式成员分离建模，便于审计与后续责任追踪；
 * 3. 删除采用逻辑禁用，避免历史账单失去引用对象。
 */
@Getter
@Builder
public class TempParticipant {
    private Long id;
    private Long bookId;
    private String nickname;
    private TempParticipantType tempType;
    private Long createdByMemberId;
    private Long attachedMemberId;
    private TempParticipantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return status == TempParticipantStatus.ACTIVE;
    }

    public boolean isPrivateScope() {
        return TempParticipantType.normalize(tempType) == TempParticipantType.PRIVATE;
    }

    public boolean isGlobal() {
        return TempParticipantType.normalize(tempType) == TempParticipantType.GLOBAL;
    }

    /**
     * 生成昵称更新后的快照。
     * 临时成员的类型、可见范围和创建人不允许在该接口中被修改。
     */
    public TempParticipant rename(String newNickname) {
        return rebuild(
            StringUtils.hasText(newNickname) ? newNickname.trim() : nickname,
            tempType,
            createdByMemberId,
            attachedMemberId,
            status
        );
    }

    /**
     * 调整当前挂靠正式成员。
     * 该行为不会改变创建人，只会把责任归属切换到新的正式成员。
     */
    public TempParticipant changeAttachedMember(Long newAttachedMemberId) {
        return rebuild(nickname, tempType, createdByMemberId, newAttachedMemberId, status);
    }

    /**
     * 逻辑删除临时成员。
     * 业务上保留历史数据，因此此处仅将状态改为 DISABLED。
     */
    public TempParticipant disable() {
        return rebuild(nickname, tempType, createdByMemberId, attachedMemberId, TempParticipantStatus.DISABLED);
    }

    /**
     * 重新启用临时成员。
     */
    public TempParticipant enable() {
        return rebuild(nickname, tempType, createdByMemberId, attachedMemberId, TempParticipantStatus.ACTIVE);
    }

    private TempParticipant rebuild(
        String targetNickname,
        TempParticipantType targetType,
        Long targetCreatedByMemberId,
        Long targetAttachedMemberId,
        TempParticipantStatus targetStatus
    ) {
        return TempParticipant.builder()
            .id(id)
            .bookId(bookId)
            .nickname(targetNickname)
            .tempType(targetType)
            .createdByMemberId(targetCreatedByMemberId)
            .attachedMemberId(targetAttachedMemberId)
            .status(targetStatus)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }
}
