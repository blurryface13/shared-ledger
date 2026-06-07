package com.spvermicelli.tripledger.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.RefreshTokenSessionMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.UserMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.RefreshTokenSessionPO;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.UserPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookInvitationMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookMemberMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookInvitationPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookMemberPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookPO;
import com.spvermicelli.tripledger.shared.domain.enums.BookStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BookType;
import com.spvermicelli.tripledger.shared.domain.enums.InvitationStatus;
import com.spvermicelli.tripledger.shared.domain.enums.MemberRole;
import com.spvermicelli.tripledger.shared.domain.enums.MemberStatus;
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
class LedgerMemberInvitationIntegrationTest {

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
    private BookInvitationMapper bookInvitationMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> bookIds = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();
    private final List<Long> invitationIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        if (!invitationIds.isEmpty()) {
            bookInvitationMapper.deleteByIds(invitationIds);
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
    void shouldCreatePendingInvitationListItAndAccept() throws Exception {
        String ownerCode = "invite-owner-" + UUID.randomUUID();
        String inviteeCode = "invite-invitee-" + UUID.randomUUID();
        UserPO owner = insertUserForCode(ownerCode, "邀请人", "13812340001");
        UserPO invitee = insertUserForCode(inviteeCode, "被邀请人", "13812340002");
        BookPO book = insertBook("春节分摊账本", BookType.SHARED, owner.getId());
        BookMemberPO ownerMember = insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());

        String ownerToken = loginAndExtractToken(ownerCode);
        String inviteeToken = loginAndExtractToken(inviteeCode);

        String createResponse = mockMvc.perform(post("/api/v1/books/" + book.getId() + "/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "inviteeUserId": %s,
                      "remark": "来一起记账吧"
                    }
                    """.formatted(invitee.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.message").value("邀请已发送，对方需在 24 小时内处理"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long invitationId = extractLongField(createResponse, "invitationId");
        invitationIds.add(invitationId);

        mockMvc.perform(get("/api/v1/invitations/pending")
                .header("Authorization", "Bearer " + inviteeToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].invitationId").value(invitationId))
            .andExpect(jsonPath("$.data[0].bookName").value("春节分摊账本"))
            .andExpect(jsonPath("$.data[0].inviterNickname").value("邀请人"))
            .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept")
                .header("Authorization", "Bearer " + inviteeToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.data.message").value("你已成功加入账本"));

        BookInvitationPO invitationPO = bookInvitationMapper.selectById(invitationId);
        org.junit.jupiter.api.Assertions.assertEquals(InvitationStatus.ACCEPTED, invitationPO.getStatus());

        BookMemberPO inviteeMember = bookMemberMapper.selectOne(new LambdaQueryWrapper<BookMemberPO>()
            .eq(BookMemberPO::getBookId, book.getId())
            .eq(BookMemberPO::getUserId, invitee.getId()));
        memberIds.add(inviteeMember.getId());
        org.junit.jupiter.api.Assertions.assertEquals(MemberStatus.ACTIVE, inviteeMember.getMemberStatus());
        org.junit.jupiter.api.Assertions.assertEquals(MemberRole.MEMBER, inviteeMember.getMemberRole());

        mockMvc.perform(get("/api/v1/books/" + book.getId() + "/members")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].joinedAt").isString())
            .andExpect(jsonPath("$.data[1].nickname").value("被邀请人"))
            .andExpect(jsonPath("$.data[1].phoneNumber").value("138****0002"));

        memberIds.add(ownerMember.getId());
    }

    @Test
    void shouldRejectInvitation() throws Exception {
        String ownerCode = "reject-owner-" + UUID.randomUUID();
        String inviteeCode = "reject-invitee-" + UUID.randomUUID();
        UserPO owner = insertUserForCode(ownerCode, "发起人", "13812340011");
        UserPO invitee = insertUserForCode(inviteeCode, "待拒绝用户", "13812340012");
        BookPO book = insertBook("拒绝邀请账本", BookType.SHARED, owner.getId());
        BookMemberPO ownerMember = insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());

        String ownerToken = loginAndExtractToken(ownerCode);
        String inviteeToken = loginAndExtractToken(inviteeCode);

        String createResponse = mockMvc.perform(post("/api/v1/books/" + book.getId() + "/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "inviteeUserId": %s
                    }
                    """.formatted(invitee.getId())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long invitationId = extractLongField(createResponse, "invitationId");
        invitationIds.add(invitationId);

        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/reject")
                .header("Authorization", "Bearer " + inviteeToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("REJECTED"))
            .andExpect(jsonPath("$.data.message").value("你已拒绝该邀请"));

        BookInvitationPO invitationPO = bookInvitationMapper.selectById(invitationId);
        org.junit.jupiter.api.Assertions.assertEquals(InvitationStatus.REJECTED, invitationPO.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(bookMemberMapper.selectOne(new LambdaQueryWrapper<BookMemberPO>()
            .eq(BookMemberPO::getBookId, book.getId())
            .eq(BookMemberPO::getUserId, invitee.getId())));

        memberIds.add(ownerMember.getId());
    }

    @Test
    void shouldListSentAndReceivedInvitationsAndSupportRevoke() throws Exception {
        String ownerCode = "msg-owner-" + UUID.randomUUID();
        String pendingInviteeCode = "msg-pending-" + UUID.randomUUID();
        String acceptedInviteeCode = "msg-accepted-" + UUID.randomUUID();

        UserPO owner = insertUserForCode(ownerCode, "消息发起人", "13812340101");
        UserPO pendingInvitee = insertUserForCode(pendingInviteeCode, "待撤销用户", "13812340102");
        UserPO acceptedInvitee = insertUserForCode(acceptedInviteeCode, "已同意用户", "13812340103");

        BookPO book = insertBook("消息账本", BookType.SHARED, owner.getId());
        BookMemberPO ownerMember = insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());

        String ownerToken = loginAndExtractToken(ownerCode);
        String pendingInviteeToken = loginAndExtractToken(pendingInviteeCode);
        String acceptedInviteeToken = loginAndExtractToken(acceptedInviteeCode);

        String pendingCreateResponse = mockMvc.perform(post("/api/v1/books/" + book.getId() + "/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "inviteeUserId": %s,
                      "remark": "请加入账本"
                    }
                    """.formatted(pendingInvitee.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long pendingInvitationId = extractLongField(pendingCreateResponse, "invitationId");
        invitationIds.add(pendingInvitationId);

        String acceptedCreateResponse = mockMvc.perform(post("/api/v1/books/" + book.getId() + "/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "inviteeUserId": %s
                    }
                    """.formatted(acceptedInvitee.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long acceptedInvitationId = extractLongField(acceptedCreateResponse, "invitationId");
        invitationIds.add(acceptedInvitationId);

        mockMvc.perform(post("/api/v1/invitations/" + acceptedInvitationId + "/accept")
                .header("Authorization", "Bearer " + acceptedInviteeToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        BookMemberPO acceptedMember = bookMemberMapper.selectOne(new LambdaQueryWrapper<BookMemberPO>()
            .eq(BookMemberPO::getBookId, book.getId())
            .eq(BookMemberPO::getUserId, acceptedInvitee.getId()));
        memberIds.add(acceptedMember.getId());

        mockMvc.perform(get("/api/v1/invitations/sent")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].bookName").value("消息账本"));

        mockMvc.perform(post("/api/v1/invitations/" + pendingInvitationId + "/revoke")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("REVOKED"))
            .andExpect(jsonPath("$.data.message").value("邀请已撤销"));

        mockMvc.perform(post("/api/v1/invitations/" + pendingInvitationId + "/accept")
                .header("Authorization", "Bearer " + pendingInviteeToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4006))
            .andExpect(jsonPath("$.message").value("该邀请已被撤销，请勿重复处理"));

        mockMvc.perform(get("/api/v1/invitations/received")
                .header("Authorization", "Bearer " + pendingInviteeToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].status").value("REVOKED"))
            .andExpect(jsonPath("$.data[0].inviterNickname").value("消息发起人"));

        memberIds.add(ownerMember.getId());
    }

    @Test
    void shouldQuitAndRemoveMembersWithPermissionChecks() throws Exception {
        String ownerCode = "quit-owner-" + UUID.randomUUID();
        String adminCode = "quit-admin-" + UUID.randomUUID();
        String memberCode = "quit-member-" + UUID.randomUUID();
        UserPO owner = insertUserForCode(ownerCode, "账本创建者", "13812340021");
        UserPO admin = insertUserForCode(adminCode, "账本管理员", "13812340022");
        UserPO member = insertUserForCode(memberCode, "普通成员", "13812340023");
        BookPO book = insertBook("成员管理账本", BookType.SHARED, owner.getId());
        BookMemberPO ownerMember = insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());
        BookMemberPO adminMember = insertMember(book.getId(), admin.getId(), MemberRole.ADMIN, MemberStatus.ACTIVE, owner.getId());
        BookMemberPO normalMember = insertMember(book.getId(), member.getId(), MemberRole.MEMBER, MemberStatus.ACTIVE, owner.getId());

        String ownerToken = loginAndExtractToken(ownerCode);
        String adminToken = loginAndExtractToken(adminCode);
        String memberToken = loginAndExtractToken(memberCode);

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/quit")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("你已成功退出账本"));

        org.junit.jupiter.api.Assertions.assertEquals(MemberStatus.QUIT,
            bookMemberMapper.selectById(normalMember.getId()).getMemberStatus());

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/quit")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4007))
            .andExpect(jsonPath("$.message").value("创建者不能直接退出账本，请先转让创建者身份"));

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/members/" + ownerMember.getId() + "/remove")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("创建者不能被移除"));

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/members/" + adminMember.getId() + "/remove")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("成员已移出账本"));

        org.junit.jupiter.api.Assertions.assertEquals(MemberStatus.REMOVED,
            bookMemberMapper.selectById(adminMember.getId()).getMemberStatus());

        memberIds.add(ownerMember.getId());
        memberIds.add(adminMember.getId());
        memberIds.add(normalMember.getId());
    }

    @Test
    void shouldSetAdminCancelAdminAndTransferOwner() throws Exception {
        String ownerCode = "role-owner-" + UUID.randomUUID();
        String targetAdminCode = "role-target-" + UUID.randomUUID();
        String newOwnerCode = "role-new-owner-" + UUID.randomUUID();
        UserPO owner = insertUserForCode(ownerCode, "原创建者", "13812340031");
        UserPO targetAdmin = insertUserForCode(targetAdminCode, "目标成员", "13812340032");
        UserPO newOwner = insertUserForCode(newOwnerCode, "新创建者", "13812340033");
        BookPO book = insertBook("角色切换账本", BookType.SHARED, owner.getId());
        BookMemberPO ownerMember = insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());
        BookMemberPO targetAdminMember = insertMember(book.getId(), targetAdmin.getId(), MemberRole.MEMBER, MemberStatus.ACTIVE, owner.getId());
        BookMemberPO newOwnerMember = insertMember(book.getId(), newOwner.getId(), MemberRole.MEMBER, MemberStatus.ACTIVE, owner.getId());

        String ownerToken = loginAndExtractToken(ownerCode);
        String targetAdminToken = loginAndExtractToken(targetAdminCode);

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/members/" + targetAdminMember.getId() + "/set-admin")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("已设置为管理员"));

        org.junit.jupiter.api.Assertions.assertEquals(MemberRole.ADMIN,
            bookMemberMapper.selectById(targetAdminMember.getId()).getMemberRole());

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/members/" + newOwnerMember.getId() + "/set-admin")
                .header("Authorization", "Bearer " + targetAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("仅账本创建者可以执行该操作"));

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/members/" + targetAdminMember.getId() + "/cancel-admin")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("已取消管理员身份"));

        org.junit.jupiter.api.Assertions.assertEquals(MemberRole.MEMBER,
            bookMemberMapper.selectById(targetAdminMember.getId()).getMemberRole());

        mockMvc.perform(post("/api/v1/books/" + book.getId() + "/transfer-owner")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "targetMemberId": %s
                    }
                    """.formatted(newOwnerMember.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("创建者身份已成功转让"));

        org.junit.jupiter.api.Assertions.assertEquals(newOwner.getId(), bookMapper.selectById(book.getId()).getOwnerUserId());
        org.junit.jupiter.api.Assertions.assertEquals(MemberRole.MEMBER,
            bookMemberMapper.selectById(ownerMember.getId()).getMemberRole());
        org.junit.jupiter.api.Assertions.assertEquals(MemberRole.OWNER,
            bookMemberMapper.selectById(newOwnerMember.getId()).getMemberRole());

        memberIds.add(ownerMember.getId());
        memberIds.add(targetAdminMember.getId());
        memberIds.add(newOwnerMember.getId());
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
