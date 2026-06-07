package com.spvermicelli.tripledger.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookMemberMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.TempParticipantMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookMemberPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.TempParticipantPO;
import com.spvermicelli.tripledger.shared.domain.enums.BookStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BookType;
import com.spvermicelli.tripledger.shared.domain.enums.MemberRole;
import com.spvermicelli.tripledger.shared.domain.enums.MemberStatus;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantStatus;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import com.spvermicelli.tripledger.shared.domain.enums.UserStatus;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.mapper.OperationLogMapper;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.OperationLogPO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
class LedgerTempParticipantIntegrationTest {

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
    private TempParticipantMapper tempParticipantMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> bookIds = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();
    private final List<Long> tempParticipantIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        if (!tempParticipantIds.isEmpty()) {
            tempParticipantMapper.deleteByIds(tempParticipantIds);
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
    void shouldListOnlyVisibleTempParticipantsAndKeepDisabledItems() throws Exception {
        String ownerCode = randomLoginCode("temp-owner");
        String creatorCode = randomLoginCode("temp-creator");
        String attachedCode = randomLoginCode("temp-attached");

        UserPO owner = insertUserForCode(ownerCode, "账本创建者", "13812342001");
        UserPO creator = insertUserForCode(creatorCode, "创建成员", "13812342002");
        UserPO attached = insertUserForCode(attachedCode, "挂靠成员", "13812342003");

        BookPO book = insertBook("临时成员账本", BookType.SHARED, owner.getId());
        BookMemberPO ownerMember = insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());
        BookMemberPO creatorMember = insertMember(book.getId(), creator.getId(), MemberRole.MEMBER, MemberStatus.ACTIVE, owner.getId());
        BookMemberPO attachedMember = insertMember(book.getId(), attached.getId(), MemberRole.MEMBER, MemberStatus.ACTIVE, owner.getId());

        insertTempParticipant(book.getId(), "公开临时成员", TempParticipantType.GLOBAL,
            creatorMember.getId(), attachedMember.getId(), TempParticipantStatus.ACTIVE);
        insertTempParticipant(book.getId(), "私有临时成员", TempParticipantType.PRIVATE,
            creatorMember.getId(), attachedMember.getId(), TempParticipantStatus.ACTIVE);
        insertTempParticipant(book.getId(), "已禁用临时成员", TempParticipantType.PRIVATE,
            creatorMember.getId(), attachedMember.getId(), TempParticipantStatus.DISABLED);

        String ownerToken = loginAndExtractToken(ownerCode);
        String attachedToken = loginAndExtractToken(attachedCode);

        mockMvc.perform(get("/api/v1/books/" + book.getId() + "/temp-participants")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[?(@.nickname=='公开临时成员')]", Matchers.hasSize(1)))
            .andExpect(jsonPath("$.data[?(@.nickname=='私有临时成员')]", Matchers.hasSize(0)))
            .andExpect(jsonPath("$.data[?(@.nickname=='已禁用临时成员')]", Matchers.hasSize(0)));

        mockMvc.perform(get("/api/v1/books/" + book.getId() + "/temp-participants")
                .param("tempType", "GLOBAL")
                .header("Authorization", "Bearer " + attachedToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[?(@.nickname=='公开临时成员')]", Matchers.hasSize(1)))
            .andExpect(jsonPath("$.data[?(@.nickname=='已禁用临时成员')]", Matchers.hasSize(0)))
            .andExpect(jsonPath("$.data[?(@.nickname=='公开临时成员')].createdByMember.memberId",
                Matchers.hasItem(creatorMember.getId().intValue())))
            .andExpect(jsonPath("$.data[?(@.nickname=='公开临时成员')].attachedMember.memberId",
                Matchers.hasItem(attachedMember.getId().intValue())))
            .andExpect(jsonPath("$.data[?(@.nickname=='公开临时成员')].attachedMember.phoneNumber",
                Matchers.hasItem("138****2003")))
            .andExpect(jsonPath("$.data[?(@.nickname=='私有临时成员')]", Matchers.hasSize(0)));
    }

    @Test
    void shouldCreateTempParticipantAndRejectDuplicateOrInvalidAttachedMember() throws Exception {
        String ownerCode = randomLoginCode("temp-create-owner");
        String memberCode = randomLoginCode("temp-create-member");
        String outsiderCode = randomLoginCode("temp-create-out");

        UserPO owner = insertUserForCode(ownerCode, "创建者", "13812342011");
        UserPO member = insertUserForCode(memberCode, "普通成员", "13812342012");
        insertUserForCode(outsiderCode, "局外人", "13812342013");

        BookPO book = insertBook("创建临时成员账本", BookType.SHARED, owner.getId());
        insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());
        BookMemberPO memberRecord = insertMember(book.getId(), member.getId(), MemberRole.MEMBER, MemberStatus.ACTIVE, owner.getId());

        String memberToken = loginAndExtractToken(memberCode);
        String outsiderToken = loginAndExtractToken(outsiderCode);

        String createResponse = mockMvc.perform(post("/api/v1/books/" + book.getId() + "/temp-participants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "nickname": "帮带小王",
                      "tempType": "PRIVATE",
                      "attachedMemberId": %s
                    }
                    """.formatted(memberRecord.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("临时成员创建成功"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long tempParticipantId = extractLongField(createResponse, "tempParticipantId");
        tempParticipantIds.add(tempParticipantId);
        TempParticipantPO created = tempParticipantMapper.selectById(tempParticipantId);
        org.junit.jupiter.api.Assertions.assertEquals(TempParticipantType.PRIVATE, TempParticipantType.normalize(created.getTempType()));
        org.junit.jupiter.api.Assertions.assertEquals(memberRecord.getId(), created.getAttachedMemberId());

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/temp-participants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "nickname": "帮带小王",
                      "tempType": "PRIVATE",
                      "attachedMemberId": %s
                    }
                    """.formatted(memberRecord.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4007))
            .andExpect(jsonPath("$.message").value("当前账本中不允许创建重复昵称的临时成员"));

        created.setStatus(TempParticipantStatus.DISABLED);
        tempParticipantMapper.updateById(created);

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/temp-participants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "nickname": "帮带小王",
                      "tempType": "PRIVATE",
                      "attachedMemberId": %s
                    }
                    """.formatted(memberRecord.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4006))
            .andExpect(jsonPath("$.message").value("该临时成员已存在但状态为禁用，请重新启用后使用"));

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/temp-participants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + outsiderToken)
                .content("""
                    {
                      "nickname": "无权限创建",
                      "tempType": "PRIVATE",
                      "attachedMemberId": %s
                    }
                    """.formatted(memberRecord.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("权限不足，您不在该账本内"));

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/temp-participants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "nickname": "挂靠无效",
                      "tempType": "GLOBAL",
                      "attachedMemberId": 999999
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4004))
            .andExpect(jsonPath("$.message").value("不存在指定的挂靠成员"));
    }

    @Test
    void shouldUpdateNicknameChangeAttachedMemberAndDisableByCreator() throws Exception {
        String ownerCode = randomLoginCode("temp-op-owner");
        String memberACode = randomLoginCode("temp-op-a");
        String memberBCode = randomLoginCode("temp-op-b");

        UserPO owner = insertUserForCode(ownerCode, "账本创建者", "13812342021");
        UserPO memberA = insertUserForCode(memberACode, "挂靠成员A", "13812342022");
        UserPO memberB = insertUserForCode(memberBCode, "挂靠成员B", "13812342023");

        BookPO book = insertBook("临时成员操作账本", BookType.SHARED, owner.getId());
        insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());
        BookMemberPO memberARecord = insertMember(book.getId(), memberA.getId(), MemberRole.MEMBER, MemberStatus.ACTIVE, owner.getId());
        BookMemberPO memberBRecord = insertMember(book.getId(), memberB.getId(), MemberRole.MEMBER, MemberStatus.ACTIVE, owner.getId());

        TempParticipantPO tempParticipant = insertTempParticipant(book.getId(), "待操作临时成员", TempParticipantType.GLOBAL,
            memberARecord.getId(), memberARecord.getId(), TempParticipantStatus.ACTIVE);

        String memberAToken = loginAndExtractToken(memberACode);
        String memberBToken = loginAndExtractToken(memberBCode);

        mockMvc.perform(put("/api/v1/books/" + book.getId() + "/temp-participants/" + tempParticipant.getId() + "/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberAToken)
                .content("""
                    {
                      "nickname": "更新后的临时成员"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("临时成员昵称更新成功"));

        mockMvc.perform(put("/api/v1/books/" + book.getId() + "/temp-participants/" + tempParticipant.getId() + "/attached-member")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberAToken)
                .content("""
                    {
                      "attachedMemberId": %s
                    }
                    """.formatted(memberBRecord.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("挂靠正式成员更新成功"));

        TempParticipantPO updated = tempParticipantMapper.selectById(tempParticipant.getId());
        org.junit.jupiter.api.Assertions.assertEquals("更新后的临时成员", updated.getNickname());
        org.junit.jupiter.api.Assertions.assertEquals(memberBRecord.getId(), updated.getAttachedMemberId());

        mockMvc.perform(delete("/api/v1/books/" + book.getId() + "/temp-participants/" + tempParticipant.getId())
                .header("Authorization", "Bearer " + memberAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("临时成员已禁用"));

        mockMvc.perform(delete("/api/v1/books/" + book.getId() + "/temp-participants/" + tempParticipant.getId())
                .header("Authorization", "Bearer " + memberBToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4006))
            .andExpect(jsonPath("$.message").value("该临时成员当前已禁用，无法继续操作"));

        org.junit.jupiter.api.Assertions.assertEquals(
            TempParticipantStatus.DISABLED,
            tempParticipantMapper.selectById(tempParticipant.getId()).getStatus()
        );
    }

    private UserPO insertUserForCode(String code, String nickname, String mobile) {
        return insertUser("mock-openid-" + code, nickname, mobile);
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

    private BookMemberPO insertMember(Long bookId, Long userId, MemberRole role, MemberStatus status, Long invitedByUserId) {
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

    private TempParticipantPO insertTempParticipant(
        Long bookId,
        String nickname,
        TempParticipantType tempType,
        Long createdByMemberId,
        Long attachedMemberId,
        TempParticipantStatus status
    ) {
        TempParticipantPO tempParticipant = new TempParticipantPO();
        tempParticipant.setBookId(bookId);
        tempParticipant.setNickname(nickname);
        tempParticipant.setTempType(tempType);
        tempParticipant.setCreatedByMemberId(createdByMemberId);
        tempParticipant.setAttachedMemberId(attachedMemberId);
        tempParticipant.setStatus(status);
        tempParticipantMapper.insert(tempParticipant);
        tempParticipantIds.add(tempParticipant.getId());
        return tempParticipant;
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
        while (Character.isWhitespace(responseBody.charAt(valueStart))) {
            valueStart++;
        }
        int valueEnd = valueStart;
        while (valueEnd < responseBody.length() && Character.isDigit(responseBody.charAt(valueEnd))) {
            valueEnd++;
        }
        return Long.parseLong(responseBody.substring(valueStart, valueEnd));
    }

    private String randomLoginCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
