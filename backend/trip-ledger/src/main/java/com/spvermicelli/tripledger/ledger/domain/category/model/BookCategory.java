package com.spvermicelli.tripledger.ledger.domain.category.model;

import com.spvermicelli.tripledger.shared.domain.enums.CategorySource;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryStatus;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 分类领域模型。
 * 分类当前承担“账本选择项配置”的职责，因此需要同时表达可见范围、来源、归属人和启停状态。
 */
@Getter
@Builder
public class BookCategory {
    private Long id;
    private Long bookId;
    private String name;
    private String icon;
    private CategoryType categoryType;
    private CategorySource categorySource;
    private CategoryStatus status;
    private Integer sortOrder;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isSystem() {
        return categorySource == CategorySource.SYSTEM;
    }

    public boolean isGlobalCustom() {
        return categorySource == CategorySource.CUSTOM_GLOBAL;
    }

    public boolean isLocalCustom() {
        return categorySource == CategorySource.CUSTOM_LOCAL;
    }

    public boolean isActive() {
        return status == CategoryStatus.ACTIVE;
    }

    /**
     * 生成更新后的分类快照。
     * 当前仅允许修改名称和图标，不允许在运行期修改分类来源、分类类型和排序值，
     * 从而避免账单历史数据出现语义漂移。
     */
    public BookCategory update(String name, String icon) {
        return BookCategory.builder()
            .id(id)
            .bookId(bookId)
            .name(StringUtils.hasText(name) ? name.trim() : this.name)
            .icon(normalizeIcon(icon, this.icon))
            .categoryType(categoryType)
            .categorySource(categorySource)
            .status(status)
            .sortOrder(sortOrder)
            .createdByUserId(createdByUserId)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    public BookCategory disable() {
        return BookCategory.builder()
            .id(id)
            .bookId(bookId)
            .name(name)
            .icon(icon)
            .categoryType(categoryType)
            .categorySource(categorySource)
            .status(CategoryStatus.DISABLED)
            .sortOrder(sortOrder)
            .createdByUserId(createdByUserId)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    private String normalizeIcon(String latestIcon, String fallbackIcon) {
        return StringUtils.hasText(latestIcon) ? latestIcon.trim() : fallbackIcon;
    }
}
