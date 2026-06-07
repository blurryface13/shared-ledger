package com.spvermicelli.tripledger.ledger.domain.book.model;

import com.spvermicelli.tripledger.shared.domain.enums.BookStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BookType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@Builder
public class Book {
    private Long id;
    private String name;
    private BookType bookType;
    private Long ownerUserId;
    private String description;
    private String coverUrl;
    private BookStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 生成一份更新后的账本快照。
     * 仅覆盖前端明确传入且非空的字段，避免未传字段被误清空。
     */
    public Book update(String name, String description, String coverUrl) {
        return Book.builder()
            .id(id)
            .name(StringUtils.hasText(name) ? name.trim() : this.name)
            .bookType(bookType)
            .ownerUserId(ownerUserId)
            .description(StringUtils.hasText(description) ? description.trim() : this.description)
            .coverUrl(StringUtils.hasText(coverUrl) ? coverUrl.trim() : this.coverUrl)
            .status(status)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    /**
     * 转让账本创建者。
     * 这里只负责变更聚合中的 ownerUserId，具体成员角色切换由应用服务协同完成。
     */
    public Book transferOwnership(Long targetOwnerUserId) {
        return Book.builder()
            .id(id)
            .name(name)
            .bookType(bookType)
            .ownerUserId(targetOwnerUserId)
            .description(description)
            .coverUrl(coverUrl)
            .status(status)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    public boolean isActive() {
        return status == BookStatus.ACTIVE;
    }

    public boolean isShared() {
        return bookType == BookType.SHARED;
    }
}
