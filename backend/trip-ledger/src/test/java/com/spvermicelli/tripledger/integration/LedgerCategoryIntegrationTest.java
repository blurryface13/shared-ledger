package com.spvermicelli.tripledger.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.RefreshTokenSessionMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.UserMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.RefreshTokenSessionPO;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.UserPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookCategoryMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookMemberMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookCategoryPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookMemberPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookPO;
import com.spvermicelli.tripledger.shared.domain.enums.BookStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BookType;
import com.spvermicelli.tripledger.shared.domain.enums.CategorySource;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryStatus;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryType;
import com.spvermicelli.tripledger.shared.domain.enums.MemberRole;
import com.spvermicelli.tripledger.shared.domain.enums.MemberStatus;
import com.spvermicelli.tripledger.shared.domain.enums.UserStatus;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.mapper.OperationLogMapper;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.OperationLogPO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.auth.enabled=true")
class LedgerCategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RefreshTokenSessionMapper refreshTokenSessionMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BookMemberMapper bookMemberMapper;

    @Autowired
    private BookCategoryMapper bookCategoryMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> bookIds = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();
    private final List<Long> categoryIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        if (!categoryIds.isEmpty()) {
            bookCategoryMapper.deleteByIds(categoryIds);
        }
        if (!bookIds.isEmpty()) {
            operationLogMapper.delete(new LambdaQueryWrapper<OperationLogPO>()
                .in(OperationLogPO::getBookId, bookIds));
        }
        if (!memberIds.isEmpty()) {
            bookMemberMapper.deleteByIds(memberIds);
        }
        if (!bookIds.isEmpty()) {
            bookMapper.deleteByIds(bookIds);
        }
        if (!userIds.isEmpty()) {
            refreshTokenSessionMapper.delete(new LambdaQueryWrapper<RefreshTokenSessionPO>()
                .in(RefreshTokenSessionPO::getUserId, userIds));
            userMapper.deleteByIds(userIds);
        }
    }

    @Test
    void shouldListSystemGlobalAndLocalCategoriesInOrder() throws Exception {
        String ownerCode = randomLoginCode("cat-owner");
        String outsiderCode = randomLoginCode("cat-out");
        UserPO owner = insertUserForCode(ownerCode, "分类拥有者", "13812341001");
        UserPO outsider = insertUserForCode(outsiderCode, "无关用户", "13812341002");

        BookPO book = insertBook("分类测试账本", BookType.SHARED, owner.getId());
        insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());

        BookCategoryPO ownerGlobal = insertCategory(book.getId(), "我的全局分类", "setting", CategoryType.EXPENSE,
            CategorySource.CUSTOM_GLOBAL, CategoryStatus.ACTIVE, 1, owner.getId());
        BookCategoryPO ownerLocal = insertCategory(book.getId(), "我的本地分类", "star", CategoryType.EXPENSE,
            CategorySource.CUSTOM_LOCAL, CategoryStatus.ACTIVE, 1, owner.getId());
        insertCategory(null, "别人全局分类", "close", CategoryType.EXPENSE,
            CategorySource.CUSTOM_GLOBAL, CategoryStatus.ACTIVE, 1, outsider.getId());

        String ownerToken = loginAndExtractToken(ownerCode);

        mockMvc.perform(get("/api/v1/books/" + book.getId() + "/categories")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].categorySource").value("SYSTEM"))
            .andExpect(jsonPath("$.data[?(@.name=='我的全局分类')]", Matchers.hasSize(1)))
            .andExpect(jsonPath("$.data[?(@.name=='我的本地分类')]", Matchers.hasSize(1)))
            .andExpect(jsonPath("$.data[?(@.name=='别人全局分类')]", Matchers.hasSize(0)))
            .andExpect(jsonPath("$.data[?(@.name=='我的全局分类')].bookId", Matchers.hasItem(book.getId().intValue())))
            .andExpect(jsonPath("$.data[?(@.name=='我的本地分类')].bookId", Matchers.hasItem(book.getId().intValue())));

        categoryIds.add(ownerGlobal.getId());
        categoryIds.add(ownerLocal.getId());
    }

    @Test
    void shouldCreateGlobalAndLocalCategoriesWithServerSideSortOrder() throws Exception {
        String ownerCode = randomLoginCode("cat-create");
        UserPO owner = insertUserForCode(ownerCode, "创建分类用户", "13812341011");
        BookPO book = insertBook("创建分类账本", BookType.SHARED, owner.getId());
        insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());

        String ownerToken = loginAndExtractToken(ownerCode);

        String globalResponse = mockMvc.perform(post("/api/v1/books/" + book.getId() + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "name": "全局收入分类",
                      "icon": "money",
                      "categoryType": "INCOME",
                      "categorySource": "CUSTOM_GLOBAL"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("分类创建成功"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long globalCategoryId = extractLongField(globalResponse, "categoryId");
        categoryIds.add(globalCategoryId);
        BookCategoryPO globalCategory = bookCategoryMapper.selectById(globalCategoryId);
        org.junit.jupiter.api.Assertions.assertEquals(book.getId(), globalCategory.getBookId());
        org.junit.jupiter.api.Assertions.assertEquals(CategorySource.CUSTOM_GLOBAL, globalCategory.getCategorySource());
        org.junit.jupiter.api.Assertions.assertEquals(1, globalCategory.getSortOrder());

        String localResponse = mockMvc.perform(post("/api/v1/books/" + book.getId() + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "name": "本地支出分类",
                      "icon": "cart",
                      "categoryType": "EXPENSE",
                      "categorySource": "CUSTOM_LOCAL"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long localCategoryId = extractLongField(localResponse, "categoryId");
        categoryIds.add(localCategoryId);
        BookCategoryPO localCategory = bookCategoryMapper.selectById(localCategoryId);
        org.junit.jupiter.api.Assertions.assertEquals(book.getId(), localCategory.getBookId());
        org.junit.jupiter.api.Assertions.assertEquals(CategorySource.CUSTOM_LOCAL, localCategory.getCategorySource());
        org.junit.jupiter.api.Assertions.assertEquals(1, localCategory.getSortOrder());

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "name": "本地支出分类",
                      "icon": "cart",
                      "categoryType": "EXPENSE",
                      "categorySource": "CUSTOM_LOCAL"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4007))
            .andExpect(jsonPath("$.message").value("当前作用域下已存在同名分类"));
    }

    @Test
    void shouldUpdateAndDisableOwnCategoryButRejectSystemCategoryChange() throws Exception {
        String ownerCode = randomLoginCode("cat-update");
        UserPO owner = insertUserForCode(ownerCode, "编辑分类用户", "13812341021");
        BookPO book = insertBook("编辑分类账本", BookType.SHARED, owner.getId());
        insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());
        BookCategoryPO localCategory = insertCategory(book.getId(), "待编辑本地分类", "star", CategoryType.EXPENSE,
            CategorySource.CUSTOM_LOCAL, CategoryStatus.ACTIVE, 1, owner.getId());

        String ownerToken = loginAndExtractToken(ownerCode);

        mockMvc.perform(put("/api/v1/books/" + book.getId() + "/categories/" + localCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "name": "更新后的本地分类",
                      "icon": "success"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("分类更新成功"));

        BookCategoryPO updatedCategory = bookCategoryMapper.selectById(localCategory.getId());
        org.junit.jupiter.api.Assertions.assertEquals("更新后的本地分类", updatedCategory.getName());
        org.junit.jupiter.api.Assertions.assertEquals("success", updatedCategory.getIcon());
        org.junit.jupiter.api.Assertions.assertEquals(1, updatedCategory.getSortOrder());

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/categories/" + localCategory.getId() + "/disable")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("分类已禁用"));

        org.junit.jupiter.api.Assertions.assertEquals(CategoryStatus.DISABLED,
            bookCategoryMapper.selectById(localCategory.getId()).getStatus());

        mockMvc.perform(get("/api/v1/books/" + book.getId() + "/categories")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[?(@.categoryId==%s)]".formatted(localCategory.getId()), Matchers.hasSize(0)));

        Long systemCategoryId = bookCategoryMapper.selectOne(new LambdaQueryWrapper<BookCategoryPO>()
            .eq(BookCategoryPO::getCategorySource, CategorySource.SYSTEM)
            .last("LIMIT 1"))
            .getId();

        mockMvc.perform(put("/api/v1/books/" + book.getId() + "/categories/" + systemCategoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "name": "试图改系统分类"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("系统默认分类不允许修改或禁用"));

        categoryIds.add(localCategory.getId());
    }

    @Test
    void shouldRejectCategoryOperationWhenUserIsNotInBook() throws Exception {
        String ownerCode = randomLoginCode("book-owner");
        String outsiderCode = randomLoginCode("book-out");
        UserPO owner = insertUserForCode(ownerCode, "账本创建者", "13812341031");
        insertUserForCode(outsiderCode, "局外人", "13812341032");
        BookPO book = insertBook("权限校验账本", BookType.SHARED, owner.getId());
        insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());

        String outsiderToken = loginAndExtractToken(outsiderCode);
        mockMvc.perform(get("/api/v1/books/" + book.getId() + "/categories")
                .header("Authorization", "Bearer " + outsiderToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("当前用户不在该账本中"));
    }

    private UserPO insertUser(String wechatOpenId, String nickname, String mobile) {
        UserPO user = new UserPO();
        user.setWechatOpenId(wechatOpenId);
        user.setNickname(nickname);
        user.setMobile(mobile);
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);
        userIds.add(user.getId());
        return user;
    }

    private UserPO insertUserForCode(String code, String nickname, String mobile) {
        return insertUser("mock-openid-" + code, nickname, mobile);
    }

    private String randomLoginCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private BookPO insertBook(String name, BookType bookType, Long ownerUserId) {
        BookPO book = new BookPO();
        book.setName(name);
        book.setBookType(bookType);
        book.setOwnerUserId(ownerUserId);
        book.setStatus(BookStatus.ACTIVE);
        bookMapper.insert(book);
        bookIds.add(book.getId());
        return book;
    }

    private BookMemberPO insertMember(
        Long bookId,
        Long userId,
        MemberRole role,
        MemberStatus status,
        Long invitedByUserId
    ) {
        BookMemberPO member = new BookMemberPO();
        member.setBookId(bookId);
        member.setUserId(userId);
        member.setMemberRole(role);
        member.setMemberStatus(status);
        member.setJoinedAt(LocalDateTime.now());
        member.setInvitedByUserId(invitedByUserId);
        bookMemberMapper.insert(member);
        memberIds.add(member.getId());
        return member;
    }

    private BookCategoryPO insertCategory(
        Long bookId,
        String name,
        String icon,
        CategoryType categoryType,
        CategorySource categorySource,
        CategoryStatus status,
        Integer sortOrder,
        Long createdByUserId
    ) {
        BookCategoryPO category = new BookCategoryPO();
        category.setBookId(bookId);
        category.setName(name);
        category.setIcon(icon);
        category.setCategoryType(categoryType);
        category.setCategorySource(categorySource);
        category.setStatus(status);
        category.setSortOrder(sortOrder);
        category.setCreatedByUserId(createdByUserId);
        bookCategoryMapper.insert(category);
        categoryIds.add(category.getId());
        return category;
    }

    private String loginAndExtractToken(String code) throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "%s"
                    }
                    """.formatted(code)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.token").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return extractStringField(responseBody, "token");
    }

    private String extractStringField(String responseBody, String fieldName) {
        String marker = "\"" + fieldName + "\":\"";
        int start = responseBody.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException(fieldName + " not found in response: " + responseBody);
        }
        int valueStart = start + marker.length();
        int valueEnd = responseBody.indexOf('"', valueStart);
        return responseBody.substring(valueStart, valueEnd);
    }

    private Long extractLongField(String responseBody, String fieldName) {
        String marker = "\"" + fieldName + "\":";
        int start = responseBody.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException(fieldName + " not found in response: " + responseBody);
        }
        int valueStart = start + marker.length();
        int valueEnd = valueStart;
        while (valueEnd < responseBody.length() && Character.isDigit(responseBody.charAt(valueEnd))) {
            valueEnd++;
        }
        return Long.parseLong(responseBody.substring(valueStart, valueEnd));
    }
}
