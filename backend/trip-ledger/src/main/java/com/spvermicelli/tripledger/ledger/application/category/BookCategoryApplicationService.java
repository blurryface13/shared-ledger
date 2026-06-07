package com.spvermicelli.tripledger.ledger.application.category;

import com.spvermicelli.tripledger.ledger.application.category.command.CreateBookCategoryCommand;
import com.spvermicelli.tripledger.ledger.application.category.command.DisableBookCategoryCommand;
import com.spvermicelli.tripledger.ledger.application.category.command.UpdateBookCategoryCommand;
import com.spvermicelli.tripledger.ledger.application.category.result.BookCategoryResult;
import com.spvermicelli.tripledger.ledger.application.category.result.CategoryOperationResult;
import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookRepository;
import com.spvermicelli.tripledger.ledger.domain.category.model.BookCategory;
import com.spvermicelli.tripledger.ledger.domain.category.repository.BookCategoryRepository;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.domain.enums.CategorySource;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryStatus;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryType;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 分类应用服务。
 * 负责在账本上下文内统一处理“系统分类 + 账本级自定义分类 + 个人可见分类”的可见性与权限校验。
 *
 * 设计约束：
 * 1. 图标统一存为字符串，前端既可以传 Wot UI icon name，也可以传图片 URL。
 * 2. CUSTOM_GLOBAL 与 CUSTOM_LOCAL 均绑定到当前账本，SYSTEM 不绑定账本。
 * 3. 排序值由后端自动分配，前端不可直接指定或修改。
 */
@Service
public class BookCategoryApplicationService {

    private final BookRepository bookRepository;
    private final BookMemberRepository bookMemberRepository;
    private final BookCategoryRepository bookCategoryRepository;

    public BookCategoryApplicationService(
        BookRepository bookRepository,
        BookMemberRepository bookMemberRepository,
        BookCategoryRepository bookCategoryRepository
    ) {
        this.bookRepository = bookRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.bookCategoryRepository = bookCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<BookCategoryResult> getBookCategories(Long currentUserId, Long bookId) {
        validateCurrentUser(currentUserId);
        requireCurrentActiveMember(bookId, currentUserId);

        // 可见性由后端统一裁决，避免前端通过拼参越权拿到他人的全局/本地分类。
        return bookCategoryRepository.findVisibleForBook(bookId, currentUserId).stream()
            .sorted(Comparator
                .comparingInt((BookCategory category) -> sourceOrder(category.getCategorySource()))
                .thenComparing(BookCategory::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BookCategory::getId))
            .map(this::toResult)
            .toList();
    }

    @Transactional
    public CategoryOperationResult createCategory(CreateBookCategoryCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        validateCreateCommand(command);
        BookMember currentMember = requireCurrentActiveMember(command.getBookId(), command.getCurrentUserId());
        ensureCreatePermission(command.getCategorySource(), currentMember);

        // 分类归属完全以后端规则为准：所有自定义分类都绑定当前账本。
        Long persistedBookId = resolvePersistedBookId(command.getBookId(), command.getCategorySource());
        ensureNameUnique(command.getCurrentUserId(), command.getCategorySource(), persistedBookId, command.getName(), null);

        // 排序值只允许由后端按当前用户、分类来源和作用域自动递增。
        int nextSortOrder = bookCategoryRepository.findNextSortOrder(
            command.getCurrentUserId(),
            command.getCategorySource(),
            persistedBookId
        );

        BookCategory savedCategory = bookCategoryRepository.save(BookCategory.builder()
            .bookId(persistedBookId)
            .name(command.getName().trim())
            .icon(normalizeIcon(command.getIcon()))
            .categoryType(command.getCategoryType())
            .categorySource(command.getCategorySource())
            .status(CategoryStatus.ACTIVE)
            .sortOrder(nextSortOrder)
            .createdByUserId(command.getCurrentUserId())
            .build());

        return CategoryOperationResult.builder()
            .categoryId(savedCategory.getId())
            .message("分类创建成功")
            .build();
    }

    @Transactional
    public CategoryOperationResult updateCategory(UpdateBookCategoryCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        validateUpdateCommand(command);
        BookMember currentMember = requireCurrentActiveMember(command.getBookId(), command.getCurrentUserId());

        BookCategory category = bookCategoryRepository.findById(command.getCategoryId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在"));
        ensureCategoryBelongsToBook(category, command.getBookId());
        ensureEditable(category);
        ensureCanOperateCategory(category, command.getCurrentUserId(), currentMember);

        String nextName = StringUtils.hasText(command.getName()) ? command.getName().trim() : category.getName();
        ensureNameUnique(command.getCurrentUserId(), category.getCategorySource(), category.getBookId(), nextName, category.getId());

        bookCategoryRepository.save(category.update(command.getName(), command.getIcon()));
        return CategoryOperationResult.builder()
            .categoryId(category.getId())
            .message("分类更新成功")
            .build();
    }

    @Transactional
    public CategoryOperationResult disableCategory(DisableBookCategoryCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getCategoryId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "categoryId 不能为空");
        }
        BookMember currentMember = requireCurrentActiveMember(command.getBookId(), command.getCurrentUserId());
        BookCategory category = bookCategoryRepository.findById(command.getCategoryId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在"));
        ensureCategoryBelongsToBook(category, command.getBookId());
        ensureEditable(category);
        ensureCanOperateCategory(category, command.getCurrentUserId(), currentMember);

        if (!category.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "该分类已经被禁用，无需重复操作");
        }

        bookCategoryRepository.save(category.disable());
        return CategoryOperationResult.builder()
            .categoryId(category.getId())
            .message("分类已禁用")
            .build();
    }

    private void validateCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
    }

    private void validateCreateCommand(CreateBookCategoryCommand command) {
        if (command.getBookId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        if (!StringUtils.hasText(command.getName())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "分类名称不能为空");
        }
        if (command.getCategoryType() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "categoryType 不能为空");
        }
        if (command.getCategorySource() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "categorySource 不能为空");
        }
        if (command.getCategorySource() == CategorySource.SYSTEM) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "系统分类只能由系统初始化创建，不能手动新增");
        }
        if (command.getCategorySource() == CategorySource.CUSTOM_GLOBAL) {
            return;
        }
        if (command.getCategorySource() != CategorySource.CUSTOM_LOCAL) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "不支持的分类来源");
        }
    }

    private void validateUpdateCommand(UpdateBookCategoryCommand command) {
        if (command.getBookId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        if (command.getCategoryId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "categoryId 不能为空");
        }
        if (!StringUtils.hasText(command.getName()) && !StringUtils.hasText(command.getIcon())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "name 和 icon 不能同时为空");
        }
    }

    private BookMember requireCurrentActiveMember(Long bookId, Long currentUserId) {
        loadActiveBook(bookId);
        return bookMemberRepository.findByBookIdAndUserId(bookId, currentUserId)
            .filter(BookMember::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "当前用户不在该账本中"));
    }

    private Book loadActiveBook(Long bookId) {
        if (bookId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账本不存在"));
        if (!book.isActive()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账本不存在");
        }
        return book;
    }

    private Long resolvePersistedBookId(Long currentBookId, CategorySource categorySource) {
        if (categorySource == CategorySource.SYSTEM) {
            return null;
        }
        return currentBookId;
    }

    private void ensureNameUnique(
        Long currentUserId,
        CategorySource categorySource,
        Long persistedBookId,
        String categoryName,
        Long excludeId
    ) {
        if (bookCategoryRepository.existsSameNameInScope(
            currentUserId,
            categorySource,
            persistedBookId,
            categoryName.trim(),
            excludeId
        )) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前作用域下已存在同名分类");
        }
    }

    private void ensureCreatePermission(CategorySource categorySource, BookMember currentMember) {
        if (categorySource != CategorySource.CUSTOM_GLOBAL) {
            return;
        }
        if (!currentMember.canManageMembers()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅账本所有者或管理员可创建账本级分类");
        }
    }

    private void ensureCategoryBelongsToBook(BookCategory category, Long currentBookId) {
        if (category.isSystem()) {
            return;
        }
        if (!currentBookId.equals(category.getBookId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该分类不属于当前账本");
        }
    }

    private void ensureCanOperateCategory(BookCategory category, Long currentUserId, BookMember currentMember) {
        if (category.isGlobalCustom()) {
            if (!currentMember.canManageMembers()) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "仅账本所有者或管理员可维护账本级分类");
            }
            return;
        }
        if (category.isLocalCustom() && !currentUserId.equals(category.getCreatedByUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅分类创建者可维护个人可见分类");
        }
    }

    private void ensureEditable(BookCategory category) {
        if (category.isSystem()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统默认分类不允许修改或禁用");
        }
    }

    private String normalizeIcon(String icon) {
        return StringUtils.hasText(icon) ? icon.trim() : null;
    }

    private int sourceOrder(CategorySource categorySource) {
        return switch (categorySource) {
            case SYSTEM -> 1;
            case CUSTOM_GLOBAL -> 2;
            case CUSTOM_LOCAL -> 3;
        };
    }

    private BookCategoryResult toResult(BookCategory category) {
        return BookCategoryResult.builder()
            .categoryId(category.getId())
            .bookId(category.getBookId())
            .name(category.getName())
            .icon(category.getIcon())
            .categoryType(category.getCategoryType().getCode())
            .categorySource(category.getCategorySource().getCode())
            .status(category.getStatus().getCode())
            .sortOrder(category.getSortOrder())
            .createdByUserId(category.getCreatedByUserId())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }
}
