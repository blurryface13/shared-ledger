package com.spvermicelli.tripledger.ledger.interfaces.rest.category;

import com.spvermicelli.tripledger.ledger.application.category.BookCategoryApplicationService;
import com.spvermicelli.tripledger.ledger.application.category.command.CreateBookCategoryCommand;
import com.spvermicelli.tripledger.ledger.application.category.command.DisableBookCategoryCommand;
import com.spvermicelli.tripledger.ledger.application.category.command.UpdateBookCategoryCommand;
import com.spvermicelli.tripledger.ledger.application.category.result.BookCategoryResult;
import com.spvermicelli.tripledger.ledger.application.category.result.CategoryOperationResult;
import com.spvermicelli.tripledger.ledger.interfaces.rest.category.request.CreateBookCategoryRequest;
import com.spvermicelli.tripledger.ledger.interfaces.rest.category.request.UpdateBookCategoryRequest;
import com.spvermicelli.tripledger.ledger.interfaces.rest.category.response.BookCategoryResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.category.response.CategoryOperationResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import com.spvermicelli.tripledger.shared.domain.enums.CategorySource;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryType;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分类接口。
 * Controller 只负责协议适配和枚举解析，实际鉴权与业务规则由应用服务统一处理。
 */
@RestController
@RequestMapping("/api/v1/books/{bookId}/categories")
public class BookCategoryController {

    private final BookCategoryApplicationService bookCategoryApplicationService;

    public BookCategoryController(BookCategoryApplicationService bookCategoryApplicationService) {
        this.bookCategoryApplicationService = bookCategoryApplicationService;
    }

    @GetMapping
    public ApiResponse<List<BookCategoryResponse>> getBookCategories(@PathVariable Long bookId) {
        List<BookCategoryResponse> response = bookCategoryApplicationService.getBookCategories(
                UserContextHolder.getUserId(),
                bookId
            ).stream()
            .map(this::toResponse)
            .toList();
        return ApiResponse.success(response);
    }

    @PostMapping
    public ApiResponse<CategoryOperationResponse> createCategory(
        @PathVariable Long bookId,
        @Valid @RequestBody CreateBookCategoryRequest request
    ) {
        CategoryOperationResult result = bookCategoryApplicationService.createCategory(CreateBookCategoryCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .name(request.getName())
            .icon(request.getIcon())
            .categoryType(parseCategoryType(request.getCategoryType()))
            .categorySource(parseCategorySource(request.getCategorySource()))
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryOperationResponse> updateCategory(
        @PathVariable Long bookId,
        @PathVariable Long categoryId,
        @Valid @RequestBody UpdateBookCategoryRequest request
    ) {
        CategoryOperationResult result = bookCategoryApplicationService.updateCategory(UpdateBookCategoryCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .categoryId(categoryId)
            .name(request.getName())
            .icon(request.getIcon())
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    @PostMapping("/{categoryId}/disable")
    public ApiResponse<CategoryOperationResponse> disableCategory(
        @PathVariable Long bookId,
        @PathVariable Long categoryId
    ) {
        CategoryOperationResult result = bookCategoryApplicationService.disableCategory(DisableBookCategoryCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .categoryId(categoryId)
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    private BookCategoryResponse toResponse(BookCategoryResult result) {
        return BookCategoryResponse.builder()
            .categoryId(result.getCategoryId())
            .bookId(result.getBookId())
            .name(result.getName())
            .icon(result.getIcon())
            .categoryType(result.getCategoryType())
            .categorySource(result.getCategorySource())
            .status(result.getStatus())
            .sortOrder(result.getSortOrder())
            .createdByUserId(result.getCreatedByUserId())
            .createdAt(result.getCreatedAt())
            .updatedAt(result.getUpdatedAt())
            .build();
    }

    private CategoryOperationResponse toOperationResponse(CategoryOperationResult result) {
        return CategoryOperationResponse.builder()
            .categoryId(result.getCategoryId())
            .message(result.getMessage())
            .build();
    }

    private CategoryType parseCategoryType(String rawType) {
        if (rawType == null) {
            return null;
        }
        try {
            return CategoryType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "categoryType 仅支持 EXPENSE 或 INCOME");
        }
    }

    private CategorySource parseCategorySource(String rawSource) {
        if (rawSource == null) {
            return null;
        }
        try {
            return CategorySource.valueOf(rawSource.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                ErrorCode.INVALID_PARAM,
                "categorySource 仅支持 SYSTEM、CUSTOM_GLOBAL、CUSTOM_LOCAL"
            );
        }
    }
}
