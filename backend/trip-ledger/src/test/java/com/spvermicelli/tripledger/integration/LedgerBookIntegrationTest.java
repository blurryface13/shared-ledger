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
import java.util.UUID;
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
class LedgerBookIntegrationTest {

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
    void shouldCreateListUpdateAndDeletePersonalBook() throws Exception {
        String code = "ledger-owner-" + UUID.randomUUID();
        UserPO owner = insertUser("mock-openid-" + code, "账本拥有者", "13812345678");
        String accessToken = loginAndExtractToken(code);

        String createResponse = mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content("""
                    {
                      "name": "我的日本旅行账本",
                      "bookType": "PERSONAL",
                      "description": "东京吃喝",
                      "coverUrl": "https://example.com/book-cover.png"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.bookId").isNumber())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long bookId = extractBookId(createResponse);
        bookIds.add(bookId);
        BookMemberPO createdOwnerMember = bookMemberMapper.selectOne(new LambdaQueryWrapper<BookMemberPO>()
            .eq(BookMemberPO::getBookId, bookId)
            .eq(BookMemberPO::getUserId, owner.getId()));
        memberIds.add(createdOwnerMember.getId());

        mockMvc.perform(get("/api/v1/books/my")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].bookId").value(bookId))
            .andExpect(jsonPath("$.data[0].name").value("我的日本旅行账本"))
            .andExpect(jsonPath("$.data[0].bookType").value("PERSONAL"))
            .andExpect(jsonPath("$.data[0].createdAt").isString());

        mockMvc.perform(get("/api/v1/books/" + bookId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.shared").value(false))
            .andExpect(jsonPath("$.data.members").isArray())
            .andExpect(jsonPath("$.data.members.length()").value(1));

        mockMvc.perform(put("/api/v1/books/" + bookId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content("""
                    {
                      "description": "东京吃喝升级版",
                      "coverUrl": "https://example.com/new-cover.png"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/books/" + bookId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("我的日本旅行账本"))
            .andExpect(jsonPath("$.data.description").value("东京吃喝升级版"))
            .andExpect(jsonPath("$.data.coverUrl").value("https://example.com/new-cover.png"));

        mockMvc.perform(delete("/api/v1/books/" + bookId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/books/my")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldAllowMemberCreateSameNameButRejectDuplicateForSameOwner() throws Exception {
        String ownerCode = "dup-owner-" + UUID.randomUUID();
        String memberCode = "dup-member-" + UUID.randomUUID();
        String sharedBookName = "海口旅行" + UUID.randomUUID().toString().substring(0, 8);

        UserPO owner = insertUser("mock-openid-" + ownerCode, "共享账本创建者", "13810000001");
        UserPO member = insertUser("mock-openid-" + memberCode, "共享账本成员", "13810000002");

        String ownerToken = loginAndExtractToken(ownerCode);
        String memberToken = loginAndExtractToken(memberCode);

        String ownerCreateResponse = mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "name": "%s",
                      "bookType": "SHARED",
                      "description": "共享账本"
                    }
                    """.formatted(sharedBookName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long sharedBookId = extractBookId(ownerCreateResponse);
        bookIds.add(sharedBookId);
        BookMemberPO ownerMember = bookMemberMapper.selectOne(new LambdaQueryWrapper<BookMemberPO>()
            .eq(BookMemberPO::getBookId, sharedBookId)
            .eq(BookMemberPO::getUserId, owner.getId()));
        memberIds.add(ownerMember.getId());

        BookMemberPO joinedMember = new BookMemberPO();
        joinedMember.setBookId(sharedBookId);
        joinedMember.setUserId(member.getId());
        joinedMember.setMemberRole(MemberRole.MEMBER);
        joinedMember.setMemberStatus(MemberStatus.ACTIVE);
        joinedMember.setJoinedAt(LocalDateTime.now());
        joinedMember.setInvitedByUserId(owner.getId());
        bookMemberMapper.insert(joinedMember);
        memberIds.add(joinedMember.getId());

        String memberCreateResponse = mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "name": "%s",
                      "bookType": "PERSONAL",
                      "description": "我自己也要一份"
                    }
                    """.formatted(sharedBookName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long memberOwnBookId = extractBookId(memberCreateResponse);
        bookIds.add(memberOwnBookId);
        BookMemberPO memberOwner = bookMemberMapper.selectOne(new LambdaQueryWrapper<BookMemberPO>()
            .eq(BookMemberPO::getBookId, memberOwnBookId)
            .eq(BookMemberPO::getUserId, member.getId()));
        memberIds.add(memberOwner.getId());

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "name": "%s",
                      "bookType": "SHARED",
                      "description": "再次创建同名账本"
                    }
                    """.formatted(sharedBookName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4007))
            .andExpect(jsonPath("$.message").value("当前账号下已存在同名账本"));
    }

    @Test
    void shouldReturnSharedBookMembersAndTempParticipants() throws Exception {
        String ownerCode = "shared-owner-" + UUID.randomUUID();
        String memberCode = "shared-member-" + UUID.randomUUID();
        String outsiderCode = "shared-outsider-" + UUID.randomUUID();

        UserPO owner = insertUser("mock-openid-" + ownerCode, "共享发起人", "13812345678");
        UserPO member = insertUser("mock-openid-" + memberCode, "共享成员", "13987654321");
        insertUser("mock-openid-" + outsiderCode, "局外人", "13700001111");

        BookPO book = new BookPO();
        book.setName("春节共享账本");
        book.setBookType(BookType.SHARED);
        book.setOwnerUserId(owner.getId());
        book.setDescription("全家出行");
        book.setCoverUrl("https://example.com/shared-cover.png");
        book.setStatus(BookStatus.ACTIVE);
        bookMapper.insert(book);
        bookIds.add(book.getId());

        BookMemberPO ownerMember = new BookMemberPO();
        ownerMember.setBookId(book.getId());
        ownerMember.setUserId(owner.getId());
        ownerMember.setMemberRole(MemberRole.OWNER);
        ownerMember.setMemberStatus(MemberStatus.ACTIVE);
        ownerMember.setJoinedAt(LocalDateTime.now());
        ownerMember.setInvitedByUserId(owner.getId());
        bookMemberMapper.insert(ownerMember);
        memberIds.add(ownerMember.getId());

        BookMemberPO sharedMember = new BookMemberPO();
        sharedMember.setBookId(book.getId());
        sharedMember.setUserId(member.getId());
        sharedMember.setMemberRole(MemberRole.MEMBER);
        sharedMember.setMemberStatus(MemberStatus.ACTIVE);
        sharedMember.setJoinedAt(LocalDateTime.now());
        sharedMember.setInvitedByUserId(owner.getId());
        bookMemberMapper.insert(sharedMember);
        memberIds.add(sharedMember.getId());

        TempParticipantPO tempParticipant = new TempParticipantPO();
        tempParticipant.setBookId(book.getId());
        tempParticipant.setNickname("小朋友挂靠");
        tempParticipant.setTempType(TempParticipantType.GLOBAL);
        tempParticipant.setCreatedByMemberId(ownerMember.getId());
        tempParticipant.setAttachedMemberId(sharedMember.getId());
        tempParticipant.setStatus(TempParticipantStatus.ACTIVE);
        tempParticipantMapper.insert(tempParticipant);
        tempParticipantIds.add(tempParticipant.getId());

        String ownerToken = loginAndExtractToken(ownerCode);
        mockMvc.perform(get("/api/v1/books/" + book.getId())
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.shared").value(true))
            .andExpect(jsonPath("$.data.members.length()").value(2))
            .andExpect(jsonPath("$.data.members[1].nickname").value("共享成员"))
            .andExpect(jsonPath("$.data.members[1].phoneNumber").value("139****4321"))
            .andExpect(jsonPath("$.data.tempParticipants.length()").value(1))
            .andExpect(jsonPath("$.data.tempParticipants[0].nickname").value("小朋友挂靠"))
            .andExpect(jsonPath("$.data.tempParticipants[0].attachedUserId").value(member.getId()))
            .andExpect(jsonPath("$.data.tempParticipants[0].attachedNickname").value("共享成员"))
            .andExpect(jsonPath("$.data.tempParticipants[0].attachedPhoneNumber").value("139****4321"));

        String outsiderToken = loginAndExtractToken(outsiderCode);
        mockMvc.perform(get("/api/v1/books/" + book.getId())
                .header("Authorization", "Bearer " + outsiderToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003));
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
        return extractToken(responseBody);
    }

    private String extractToken(String responseBody) {
        String marker = "\"token\":\"";
        int start = responseBody.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("token not found in response: " + responseBody);
        }
        int valueStart = start + marker.length();
        int valueEnd = responseBody.indexOf('"', valueStart);
        return responseBody.substring(valueStart, valueEnd);
    }

    private Long extractBookId(String responseBody) {
        String marker = "\"bookId\":";
        int start = responseBody.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("bookId not found in response: " + responseBody);
        }
        int valueStart = start + marker.length();
        int valueEnd = valueStart;
        while (valueEnd < responseBody.length() && Character.isDigit(responseBody.charAt(valueEnd))) {
            valueEnd++;
        }
        return Long.parseLong(responseBody.substring(valueStart, valueEnd));
    }
}
