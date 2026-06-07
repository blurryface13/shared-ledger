package com.spvermicelli.tripledger.identity.domain.user.model;

import com.spvermicelli.tripledger.shared.domain.enums.UserStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@Builder
public class User {
    private Long id;
    private String wechatOpenId;
    private String wechatUnionId;
    private String nickname;
    private String avatarUrl;
    private String mobile;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 生成一份更新后的用户资料快照。
     * 仅当字段有值时才覆盖原属性，避免未传字段被误清空。
     */
    public User updateProfile(String nickname, String avatarUrl, String mobile) {
        return User.builder()
            .id(id)
            .wechatOpenId(wechatOpenId)
            .wechatUnionId(wechatUnionId)
            .nickname(StringUtils.hasText(nickname) ? nickname : this.nickname)
            .avatarUrl(StringUtils.hasText(avatarUrl) ? avatarUrl : this.avatarUrl)
            .mobile(StringUtils.hasText(mobile) ? mobile : this.mobile)
            .status(status)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    /**
     * 刷新微信身份信息。
     * 当前只允许补充 unionId，不覆盖 openId。
     */
    public User refreshWechatIdentity(String latestUnionId) {
        return User.builder()
            .id(id)
            .wechatOpenId(wechatOpenId)
            .wechatUnionId(StringUtils.hasText(latestUnionId) ? latestUnionId : wechatUnionId)
            .nickname(nickname)
            .avatarUrl(avatarUrl)
            .mobile(mobile)
            .status(status)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    /**
     * 注销账号时只做逻辑注销，不做物理删除。
     */
    public User cancel() {
        return User.builder()
            .id(id)
            .wechatOpenId(wechatOpenId)
            .wechatUnionId(wechatUnionId)
            .nickname(nickname)
            .avatarUrl(avatarUrl)
            .mobile(mobile)
            .status(UserStatus.CANCELLED)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    /**
     * 重新激活注销账号。
     * 当前采用复用原账号主体的策略，避免同一 openId 产生重复用户记录。
     */
    public User reactivate() {
        return User.builder()
            .id(id)
            .wechatOpenId(wechatOpenId)
            .wechatUnionId(wechatUnionId)
            .nickname(nickname)
            .avatarUrl(avatarUrl)
            .mobile(mobile)
            .status(UserStatus.ACTIVE)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    public boolean isActive() {
        return UserStatus.ACTIVE == status;
    }
}
