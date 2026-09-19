package com.spvermicelli.tripledger.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillChangeRequestApprovalMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillChangeRequestAttachmentMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillChangeRequestMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillChangeRequestShareItemMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillChangeRequestSnapshotMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillShareItemMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestApprovalPO;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestAttachmentPO;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestPO;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestShareItemPO;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestSnapshotPO;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillPO;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillShareItemPO;
import com.spvermicelli.tripledger.export.infrastructure.persistence.mapper.ExportRecordMapper;
import com.spvermicelli.tripledger.export.infrastructure.persistence.po.ExportRecordPO;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.RefreshTokenSessionMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.UserMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.RefreshTokenSessionPO;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.UserPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookCategoryMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookMemberMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.TempParticipantMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookCategoryPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookMemberPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookPO;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.TempParticipantPO;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.PaymentConfirmAllocationMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.PaymentConfirmRecordMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.SettlementBatchMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.SettlementTransferMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.TempRecoveryAllocationMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.TempRecoveryRecordMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.PaymentConfirmAllocationPO;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.PaymentConfirmRecordPO;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.SettlementBatchPO;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.SettlementTransferPO;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.TempRecoveryAllocationPO;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.TempRecoveryRecordPO;
import com.spvermicelli.tripledger.shared.domain.enums.BillStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BookStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BookType;
import com.spvermicelli.tripledger.shared.domain.enums.CategorySource;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryStatus;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 11-17 章集成测试。
 * 这组测试不只验证单个接口可调用，更关注几个核心商业链路是否真的能串起来：
 * 1. 账单创建 -> 可见性 -> 直接修改/删除；
 * 2. 账单申请 -> 审批通过/拒绝 -> 数据实际落库；
 * 3. 统计 -> 结算 -> 支付确认 -> 临时成员回补 -> 导出。
 *
 * 说明：
 * 当前测试环境直接连本地 MySQL。为了让新加的精确分配结构在已有数据库上也能跑起来，
 * 这里用幂等 DDL 做一层兜底，避免“代码已完成但测试库结构落后”的假失败。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.auth.enabled=true")
class BillingSettlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    private TempParticipantMapper tempParticipantMapper;

    @Autowired
    private BillMapper billMapper;

    @Autowired
    private BillShareItemMapper billShareItemMapper;

    @Autowired
    private BillChangeRequestMapper billChangeRequestMapper;

    @Autowired
    private BillChangeRequestApprovalMapper billChangeRequestApprovalMapper;

    @Autowired
    private BillChangeRequestSnapshotMapper billChangeRequestSnapshotMapper;

    @Autowired
    private BillChangeRequestShareItemMapper billChangeRequestShareItemMapper;

    @Autowired
    private BillChangeRequestAttachmentMapper billChangeRequestAttachmentMapper;

    @Autowired
    private SettlementBatchMapper settlementBatchMapper;

    @Autowired
    private SettlementTransferMapper settlementTransferMapper;

    @Autowired
    private PaymentConfirmRecordMapper paymentConfirmRecordMapper;

    @Autowired
    private PaymentConfirmAllocationMapper paymentConfirmAllocationMapper;

    @Autowired
    private TempRecoveryRecordMapper tempRecoveryRecordMapper;

    @Autowired
    private TempRecoveryAllocationMapper tempRecoveryAllocationMapper;

    @Autowired
    private ExportRecordMapper exportRecordMapper;

    @Autowired
    private com.spvermicelli.tripledger.export.application.ExportApplicationService exportApplicationService;

    @Autowired
    private OperationLogMapper operationLogMapper;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> bookIds = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();
    private final List<Long> categoryIds = new ArrayList<>();
    private final List<Long> tempParticipantIds = new ArrayList<>();
    private final List<Long> billIds = new ArrayList<>();
    private final List<Long> requestIds = new ArrayList<>();
    private final List<Long> settlementBatchIds = new ArrayList<>();
    private final List<Long> settlementTransferIds = new ArrayList<>();
    private final List<Long> paymentConfirmIds = new ArrayList<>();
    private final List<Long> recoveryRecordIds = new ArrayList<>();
    private final List<Long> exportRecordIds = new ArrayList<>();

    @BeforeEach
    void ensureExtendedSchema() {
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tb_export_record' AND COLUMN_NAME='export_content_json'", Integer.class) == 0) {
            jdbcTemplate.execute("ALTER TABLE tb_export_record ADD COLUMN export_content_json LONGTEXT NULL");
        }
        Integer targetTempColumnCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(1)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'tb_bill'
                  AND COLUMN_NAME = 'target_temp_participant_id'
                """,
            Integer.class
        );
        if (targetTempColumnCount == null || targetTempColumnCount == 0) {
            jdbcTemplate.execute("""
                ALTER TABLE tb_bill
                ADD COLUMN target_temp_participant_id BIGINT NULL COMMENT '目标临时成员，仅 PERSONAL_CARRY 使用'
                """);
        }
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS tb_payment_confirm_allocation (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                payment_confirm_id BIGINT NOT NULL,
                bill_id BIGINT NOT NULL,
                allocated_amount_cent BIGINT NOT NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS tb_temp_recovery_allocation (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                recovery_record_id BIGINT NOT NULL,
                bill_id BIGINT NOT NULL,
                allocated_amount_cent BIGINT NOT NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    @AfterEach
    void tearDown() {
        if (!exportRecordIds.isEmpty()) {
            exportRecordMapper.deleteByIds(exportRecordIds);
        }
        if (!bookIds.isEmpty()) {
            operationLogMapper.delete(new LambdaQueryWrapper<OperationLogPO>()
                .in(OperationLogPO::getBookId, bookIds));
        }
        if (!recoveryRecordIds.isEmpty()) {
            tempRecoveryAllocationMapper.delete(new LambdaQueryWrapper<TempRecoveryAllocationPO>()
                .in(TempRecoveryAllocationPO::getRecoveryRecordId, recoveryRecordIds));
            tempRecoveryRecordMapper.deleteByIds(recoveryRecordIds);
        } else if (!bookIds.isEmpty()) {
            tempRecoveryRecordMapper.selectList(new LambdaQueryWrapper<TempRecoveryRecordPO>()
                    .in(TempRecoveryRecordPO::getBookId, bookIds))
                .stream()
                .map(TempRecoveryRecordPO::getId)
                .toList()
                .forEach(recoveryRecordIds::add);
            if (!recoveryRecordIds.isEmpty()) {
                tempRecoveryAllocationMapper.delete(new LambdaQueryWrapper<TempRecoveryAllocationPO>()
                    .in(TempRecoveryAllocationPO::getRecoveryRecordId, recoveryRecordIds));
                tempRecoveryRecordMapper.deleteByIds(recoveryRecordIds);
            }
        }
        if (!paymentConfirmIds.isEmpty()) {
            paymentConfirmAllocationMapper.delete(new LambdaQueryWrapper<PaymentConfirmAllocationPO>()
                .in(PaymentConfirmAllocationPO::getPaymentConfirmId, paymentConfirmIds));
            paymentConfirmRecordMapper.deleteByIds(paymentConfirmIds);
        } else if (!bookIds.isEmpty()) {
            paymentConfirmRecordMapper.selectList(new LambdaQueryWrapper<PaymentConfirmRecordPO>()
                    .in(PaymentConfirmRecordPO::getBookId, bookIds))
                .stream()
                .map(PaymentConfirmRecordPO::getId)
                .toList()
                .forEach(paymentConfirmIds::add);
            if (!paymentConfirmIds.isEmpty()) {
                paymentConfirmAllocationMapper.delete(new LambdaQueryWrapper<PaymentConfirmAllocationPO>()
                    .in(PaymentConfirmAllocationPO::getPaymentConfirmId, paymentConfirmIds));
                paymentConfirmRecordMapper.deleteByIds(paymentConfirmIds);
            }
        }
        if (!settlementTransferIds.isEmpty()) {
            settlementTransferMapper.deleteByIds(settlementTransferIds);
        } else if (!settlementBatchIds.isEmpty()) {
            settlementTransferMapper.delete(new LambdaQueryWrapper<SettlementTransferPO>()
                .in(SettlementTransferPO::getSettlementBatchId, settlementBatchIds));
        }
        if (!settlementBatchIds.isEmpty()) {
            settlementBatchMapper.deleteByIds(settlementBatchIds);
        }
        List<Long> allRequestIds = new ArrayList<>();
        if (!requestIds.isEmpty()) {
            allRequestIds.addAll(requestIds);
        }
        if (!billIds.isEmpty()) {
            billChangeRequestMapper.selectList(new LambdaQueryWrapper<BillChangeRequestPO>()
                    .in(BillChangeRequestPO::getBillId, billIds))
                .stream()
                .map(BillChangeRequestPO::getId)
                .forEach(allRequestIds::add);
        }
        allRequestIds = allRequestIds.stream().distinct().toList();
        if (!allRequestIds.isEmpty()) {
            billChangeRequestAttachmentMapper.delete(new LambdaQueryWrapper<BillChangeRequestAttachmentPO>()
                .in(BillChangeRequestAttachmentPO::getRequestId, allRequestIds));
            billChangeRequestShareItemMapper.delete(new LambdaQueryWrapper<BillChangeRequestShareItemPO>()
                .in(BillChangeRequestShareItemPO::getRequestId, allRequestIds));
            billChangeRequestSnapshotMapper.delete(new LambdaQueryWrapper<BillChangeRequestSnapshotPO>()
                .in(BillChangeRequestSnapshotPO::getRequestId, allRequestIds));
            billChangeRequestApprovalMapper.delete(new LambdaQueryWrapper<BillChangeRequestApprovalPO>()
                .in(BillChangeRequestApprovalPO::getRequestId, allRequestIds));
            billMapper.update(null, new LambdaUpdateWrapper<BillPO>()
                .set(BillPO::getLatestChangeRequestId, null)
                .in(BillPO::getLatestChangeRequestId, allRequestIds));
            billChangeRequestMapper.update(null, new LambdaUpdateWrapper<BillChangeRequestPO>()
                .set(BillChangeRequestPO::getPredecessorRequestId, null)
                .in(BillChangeRequestPO::getId, allRequestIds));
            billChangeRequestMapper.deleteByIds(allRequestIds);
        }
        if (!billIds.isEmpty()) {
            billShareItemMapper.delete(new LambdaQueryWrapper<BillShareItemPO>()
                .in(BillShareItemPO::getBillId, billIds));
            billMapper.deleteByIds(billIds);
        }
        if (!tempParticipantIds.isEmpty()) {
            tempParticipantMapper.deleteByIds(tempParticipantIds);
        }
        if (!categoryIds.isEmpty()) {
            bookCategoryMapper.deleteByIds(categoryIds);
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
    void shouldManageBillsAndBillRequestsAcrossFullLifecycle() throws Exception {
        SharedBookFixture fixture = createSharedBookFixture("账单申请链路");

        String ownerToken = loginAndExtractToken(fixture.ownerCode);
        String memberToken = loginAndExtractToken(fixture.memberCode);

        Long personalExpenseBillId = createPersonalBill(
            fixture.bookId,
            ownerToken,
            """
                {
                  "billType": "PERSONAL_EXPENSE",
                  "title": "东京地铁",
                  "billAmountCent": 1200,
                  "categoryId": %s,
                  "payerMemberId": %s,
                  "recorderMemberId": %s,
                  "billTime": "2026-04-07T10:00:00",
                  "remark": "个人交通"
                }
                """.formatted(fixture.expenseCategoryId, fixture.ownerMemberId, fixture.ownerMemberId),
            "个人账单创建成功"
        );

        Long carryBillId = createPersonalBill(
            fixture.bookId,
            ownerToken,
            """
                {
                  "billType": "PERSONAL_CARRY",
                  "title": "帮带儿童午餐",
                  "billAmountCent": 2000,
                  "categoryId": %s,
                  "payerMemberId": %s,
                  "recorderMemberId": %s,
                  "tempParticipantId": %s,
                  "billTime": "2026-04-07T11:00:00",
                  "remark": "亲子午餐"
                }
                """.formatted(fixture.expenseCategoryId, fixture.ownerMemberId, fixture.ownerMemberId, fixture.tempParticipantId),
            "个人账单创建成功"
        );

        Long sharedBillId = createSharedBill(
            fixture.bookId,
            ownerToken,
            """
                {
                  "title": "大阪酒店",
                  "billAmountCent": 9000,
                  "categoryId": %s,
                  "payerMemberId": %s,
                  "recorderMemberId": %s,
                  "billTime": "2026-04-07T12:00:00",
                  "remark": "双人一娃入住",
                  "shareItems": [
                    {
                      "participantType": "MEMBER",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 6000
                    },
                    {
                      "participantType": "TEMP_PARTICIPANT",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 3000
                    }
                  ]
                }
                """.formatted(fixture.expenseCategoryId, fixture.ownerMemberId, fixture.ownerMemberId,
                fixture.memberMemberId, fixture.tempParticipantId),
            "均摊账单创建成功"
        );

        Long requestDeleteBillId = createSharedBill(
            fixture.bookId,
            ownerToken,
            """
                {
                  "title": "待删除共享账单",
                  "billAmountCent": 3000,
                  "categoryId": %s,
                  "payerMemberId": %s,
                  "recorderMemberId": %s,
                  "billTime": "2026-04-07T13:00:00",
                  "remark": "后续用于删除申请",
                  "shareItems": [
                    {
                      "participantType": "MEMBER",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 3000
                    }
                  ]
                }
                """.formatted(fixture.expenseCategoryId, fixture.ownerMemberId, fixture.ownerMemberId, fixture.memberMemberId),
            "均摊账单创建成功"
        );

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills")
                .param("pageNo", "1")
                .param("pageSize", "20")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").value(4))
            .andExpect(jsonPath("$.data.list.length()").value(4));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId)
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.billType").value("SHARED_EXPENSE"))
            .andExpect(jsonPath("$.data.directEditable").value(false))
            .andExpect(jsonPath("$.data.participants.length()").value(2))
            .andExpect(jsonPath("$.data.viewerAttachedTempShareAmountCent").value(3000))
            .andExpect(jsonPath("$.data.viewerOwnShareAmountCent").value(6000));

        mockMvc.perform(put("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "billType": "SHARED_EXPENSE",
                      "title": "大阪酒店升级版",
                      "billAmountCent": 9000,
                      "categoryId": %s,
                      "payerMemberId": %s,
                      "recorderMemberId": %s,
                      "billTime": "2026-04-07T12:30:00",
                      "remark": "升级海景房",
                      "shareItems": [
                        {
                          "participantType": "MEMBER",
                          "participantRefId": %s,
                          "shareMethod": "FIXED_AMOUNT",
                          "shareAmountCent": 6000
                        },
                        {
                          "participantType": "TEMP_PARTICIPANT",
                          "participantRefId": %s,
                          "shareMethod": "FIXED_AMOUNT",
                          "shareAmountCent": 3000
                        }
                      ]
                    }
                    """.formatted(fixture.expenseCategoryId, fixture.ownerMemberId, fixture.ownerMemberId,
                    fixture.memberMemberId, fixture.tempParticipantId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("账单更新成功"));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("大阪酒店升级版"))
            .andExpect(jsonPath("$.data.remark").value("升级海景房"));

        String modifyRequestResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId + "/modify-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "requestReason": "希望补充更清晰的标题",
                      "billType": "SHARED_EXPENSE",
                      "title": "大阪酒店最终标题",
                      "billAmountCent": 9000,
                      "categoryId": %s,
                      "payerMemberId": %s,
                      "recorderMemberId": %s,
                      "billTime": "2026-04-07T12:30:00",
                      "remark": "成员发起修改申请",
                      "shareItems": [
                        {
                          "participantType": "MEMBER",
                          "participantRefId": %s,
                          "shareMethod": "FIXED_AMOUNT",
                          "shareAmountCent": 6000
                        },
                        {
                          "participantType": "TEMP_PARTICIPANT",
                          "participantRefId": %s,
                          "shareMethod": "FIXED_AMOUNT",
                          "shareAmountCent": 3000
                        }
                      ]
                    }
                    """.formatted(fixture.expenseCategoryId, fixture.ownerMemberId, fixture.ownerMemberId,
                    fixture.memberMemberId, fixture.tempParticipantId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("账单修改申请已发起，等待审批"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long modifyRequestId = extractLongField(modifyRequestResponse, "requestId");
        requestIds.add(modifyRequestId);

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bill-requests/my")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].requestId").value(modifyRequestId));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bill-requests/pending-approve")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].currentUserCanApprove").value(true));

        mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/bill-requests/" + modifyRequestId + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "approvalComment": "标题描述更清楚，批准"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("申请已审批通过并完成执行"));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("大阪酒店最终标题"))
            .andExpect(jsonPath("$.data.remark").value("成员发起修改申请"));

        String rejectRequestResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId + "/modify-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "requestReason": "再次尝试修改为不合适标题",
                      "billType": "SHARED_EXPENSE",
                      "title": "大阪酒店待拒绝标题",
                      "billAmountCent": 9000,
                      "categoryId": %s,
                      "payerMemberId": %s,
                      "recorderMemberId": %s,
                      "billTime": "2026-04-07T12:30:00",
                      "remark": "这次会被拒绝",
                      "shareItems": [
                        {
                          "participantType": "MEMBER",
                          "participantRefId": %s,
                          "shareMethod": "FIXED_AMOUNT",
                          "shareAmountCent": 6000
                        },
                        {
                          "participantType": "TEMP_PARTICIPANT",
                          "participantRefId": %s,
                          "shareMethod": "FIXED_AMOUNT",
                          "shareAmountCent": 3000
                        }
                      ]
                    }
                    """.formatted(fixture.expenseCategoryId, fixture.ownerMemberId, fixture.ownerMemberId,
                    fixture.memberMemberId, fixture.tempParticipantId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long rejectRequestId = extractLongField(rejectRequestResponse, "requestId");
        requestIds.add(rejectRequestId);

        mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/bill-requests/" + rejectRequestId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "approvalComment": "当前标题不符合账单命名规范"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("申请已拒绝"));

        String deleteRequestResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/bills/" + requestDeleteBillId + "/delete-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "requestReason": "这笔测试账单不再需要"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("账单删除申请已发起，等待审批"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long deleteRequestId = extractLongField(deleteRequestResponse, "requestId");
        requestIds.add(deleteRequestId);

        mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/bill-requests/" + deleteRequestId + "/approve")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("申请已审批通过并完成执行"));

        mockMvc.perform(delete("/api/v1/books/" + fixture.bookId + "/bills/" + personalExpenseBillId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value("账单删除成功"));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills")
                .param("pageNo", "1")
                .param("pageSize", "20")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2));

        BillPO deletedBill = billMapper.selectById(requestDeleteBillId);
        org.junit.jupiter.api.Assertions.assertEquals(BillStatus.DELETED, deletedBill.getStatus());
    }

    @Test
    void shouldHideSharedBillFromOwnerWhenOwnerIsNotPayerOrParticipant() throws Exception {
        SharedBookFixture fixture = createSharedBookFixture("共享可见性边界");

        String ownerToken = loginAndExtractToken(fixture.ownerCode);
        String memberToken = loginAndExtractToken(fixture.memberCode);

        Long sharedBillId = createSharedBill(
            fixture.bookId,
            memberToken,
            """
                {
                  "title": "仅成员参与账单",
                  "billAmountCent": 9000,
                  "categoryId": %s,
                  "payerMemberId": %s,
                  "recorderMemberId": %s,
                  "billTime": "2026-04-07T20:00:00",
                  "remark": "owner 仅作为记录人，不参与分摊",
                  "shareItems": [
                    {
                      "participantType": "MEMBER",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 6000
                    },
                    {
                      "participantType": "TEMP_PARTICIPANT",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 3000
                    }
                  ]
                }
                """.formatted(
                fixture.expenseCategoryId,
                fixture.memberMemberId,
                fixture.ownerMemberId,
                fixture.memberMemberId,
                fixture.tempParticipantId
            ),
            "均摊账单创建成功"
        );

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills")
                .param("pageNo", "1")
                .param("pageSize", "20")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").value(0))
            .andExpect(jsonPath("$.data.list.length()").value(0));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4005))
            .andExpect(jsonPath("$.message").value("当前用户不可查看该账单"));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId)
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.billType").value("SHARED_EXPENSE"));
    }

    @Test
    void shouldIncludeAttachedTempReceivableForSharedPayer() throws Exception {
        SharedBookFixture fixture = createSharedBookFixture("共享账单挂靠应收");
        String ownerToken = loginAndExtractToken(fixture.ownerCode);

        TempParticipantPO ownerAttachedTemp = insertTempParticipant(
            fixture.bookId,
            "挂靠拥有者临时成员",
            TempParticipantType.GLOBAL,
            fixture.ownerMemberId,
            fixture.ownerMemberId,
            TempParticipantStatus.ACTIVE
        );

        Long sharedBillId = createSharedBill(
            fixture.bookId,
            ownerToken,
            """
                {
                  "title": "挂靠应收校验",
                  "billAmountCent": 30000,
                  "categoryId": %s,
                  "payerMemberId": %s,
                  "recorderMemberId": %s,
                  "billTime": "2026-04-12T12:00:00",
                  "remark": "校验挂靠临时成员应收",
                  "shareItems": [
                    {
                      "participantType": "MEMBER",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 10000
                    },
                    {
                      "participantType": "MEMBER",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 10000
                    },
                    {
                      "participantType": "TEMP_PARTICIPANT",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 10000
                    }
                  ]
                }
                """.formatted(
                fixture.expenseCategoryId,
                fixture.ownerMemberId,
                fixture.ownerMemberId,
                fixture.ownerMemberId,
                fixture.memberMemberId,
                ownerAttachedTemp.getId()
            ),
            "均摊账单创建成功"
        );

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.viewerReceivableAmountCent").value(20000))
            .andExpect(jsonPath("$.data.viewerRecoveredAmountCent").value(0))
            .andExpect(jsonPath("$.data.viewerUnrecoveredAmountCent").value(20000));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills")
                .param("pageNo", "1")
                .param("pageSize", "20")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.list[0].billId").value(sharedBillId))
            .andExpect(jsonPath("$.data.list[0].viewerUnrecoveredAmountCent").value(20000));
    }

    @Test
    void shouldRunStatisticsSettlementPaymentRecoveryAndExportFlow() throws Exception {
        SharedBookFixture fixture = createSharedBookFixture("统计结算链路");

        String ownerToken = loginAndExtractToken(fixture.ownerCode);
        String memberToken = loginAndExtractToken(fixture.memberCode);

        Long sharedBillId = createSharedBill(
            fixture.bookId,
            ownerToken,
            """
                {
                  "title": "京都酒店",
                  "billAmountCent": 9000,
                  "categoryId": %s,
                  "payerMemberId": %s,
                  "recorderMemberId": %s,
                  "billTime": "2026-04-07T15:00:00",
                  "remark": "共享住宿",
                  "shareItems": [
                    {
                      "participantType": "MEMBER",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 6000
                    },
                    {
                      "participantType": "TEMP_PARTICIPANT",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 3000
                    }
                  ]
                }
                """.formatted(fixture.expenseCategoryId, fixture.ownerMemberId, fixture.ownerMemberId,
                fixture.memberMemberId, fixture.tempParticipantId),
            "均摊账单创建成功"
        );

        createPersonalBill(
            fixture.bookId,
            ownerToken,
            """
                {
                  "billType": "PERSONAL_CARRY",
                  "title": "帮带景区门票",
                  "billAmountCent": 2000,
                  "categoryId": %s,
                  "payerMemberId": %s,
                  "recorderMemberId": %s,
                  "tempParticipantId": %s,
                  "billTime": "2026-04-07T16:00:00",
                  "remark": "儿童门票"
                }
                """.formatted(fixture.expenseCategoryId, fixture.ownerMemberId, fixture.ownerMemberId, fixture.tempParticipantId),
            "个人账单创建成功"
        );

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/statistics/overview")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.payFlowAmountCent").isNumber())
            .andExpect(jsonPath("$.data.pendingContributionAmountCent").isNumber());

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/statistics/category-consumption")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].categoryId").value(fixture.expenseCategoryId));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/statistics/member-relations")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].targetParticipantType").value("MEMBER"))
            .andExpect(jsonPath("$.data[0].targetMemberId").value(fixture.ownerMemberId))
            .andExpect(jsonPath("$.data[0].netAmountCent").value(11000))
            .andExpect(jsonPath("$.data[1].targetParticipantType").value("TEMP_PARTICIPANT"))
            .andExpect(jsonPath("$.data[1].targetParticipantId").value(fixture.tempParticipantId));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/statistics/attached-temp-details")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].tempParticipantId").value(fixture.tempParticipantId))
            .andExpect(jsonPath("$.data[0].unrecoveredAmountCent").value(5000));

        String settlementResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/settlements")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "strategyType": "INTUITIVE_FIRST"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.transferList.length()").value(1))
            .andExpect(jsonPath("$.data.transferList[0].transferAmountCent").value(11000))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long settlementBatchId = extractLongField(settlementResponse, "settlementBatchId");
        Long settlementTransferId = extractLongField(settlementResponse, "transferId");
        settlementBatchIds.add(settlementBatchId);
        settlementTransferIds.add(settlementTransferId);

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/settlements/" + settlementBatchId)
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.settlementBatchId").value(settlementBatchId));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/settlements")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1));

        String paymentConfirmResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/payment-confirms")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "toMemberId": %s,
                      "paymentAmountCent": 4000,
                      "sourceType": "SETTLEMENT_TRANSFER",
                      "sourceRefId": %s,
                      "remark": "先支付一部分"
                    }
                    """.formatted(fixture.ownerMemberId, settlementTransferId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.confirmStatus").value("PENDING_CONFIRM"))
            .andExpect(jsonPath("$.data.allocationList.length()").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long paymentConfirmId = extractLongField(paymentConfirmResponse, "paymentConfirmId");
        paymentConfirmIds.add(paymentConfirmId);

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/payment-confirms/pending-pay")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/payment-confirms/pending-receive")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/payment-confirms/" + paymentConfirmId + "/confirm")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.confirmStatus").value("CONFIRMED"));

        String directReceiveResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/payment-confirms/direct-receive")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "fromMemberId": %s,
                      "paymentAmountCent": 1000,
                      "sourceType": "MANUAL",
                      "remark": "线下现金"
                    }
                    """.formatted(fixture.memberMemberId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.confirmStatus").value("CONFIRMED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        paymentConfirmIds.add(extractLongField(directReceiveResponse, "paymentConfirmId"));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.viewerRecoveredAmountCent").value(5000))
            .andExpect(jsonPath("$.data.viewerUnrecoveredAmountCent").value(4000));

        org.junit.jupiter.api.Assertions.assertFalse(paymentConfirmAllocationMapper.selectList(
            new LambdaQueryWrapper<PaymentConfirmAllocationPO>()
                .eq(PaymentConfirmAllocationPO::getPaymentConfirmId, paymentConfirmId)
        ).isEmpty());

        String recoveryResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/temp-recoveries")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "tempParticipantId": %s,
                      "recoveryAmountCent": 1000,
                      "remark": "家长已转账"
                    }
                    """.formatted(fixture.tempParticipantId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.confirmStatus").value("CONFIRMED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long recoveryRecordId = extractLongField(recoveryResponse, "recoveryRecordId");
        recoveryRecordIds.add(recoveryRecordId);

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/temp-recoveries")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].allocationList.length()").value(1));

        org.junit.jupiter.api.Assertions.assertFalse(tempRecoveryAllocationMapper.selectList(
            new LambdaQueryWrapper<TempRecoveryAllocationPO>()
                .eq(TempRecoveryAllocationPO::getRecoveryRecordId, recoveryRecordId)
        ).isEmpty());

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/statistics/attached-temp-details")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].recoveredAmountCent").value(1000))
            .andExpect(jsonPath("$.data[0].unrecoveredAmountCent").value(4000));

        String memberExportResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/exports/personal-detail")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.exportType").value("PERSONAL_DETAIL"))
            .andExpect(jsonPath("$.data.exportStatus").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        exportRecordIds.add(extractLongField(memberExportResponse, "exportRecordId"));
        awaitExportSnapshot(exportRecordIds.getLast());

        mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/exports/book-summary")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("仅账本创建者可以导出全账本消费汇总"));

        String ownerExportResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/exports/book-summary")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.exportType").value("BOOK_SUMMARY"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        exportRecordIds.add(extractLongField(ownerExportResponse, "exportRecordId"));
        awaitExportSnapshot(exportRecordIds.getLast());

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/exports")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/exports")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldRejectCrossBookUnauthorizedAccessAndUnsafeSettlementOperations() throws Exception {
        SharedBookFixture fixture = createSharedBookFixture("权限边界链路");

        String ownerToken = loginAndExtractToken(fixture.ownerCode);
        String memberToken = loginAndExtractToken(fixture.memberCode);
        String outsiderToken = loginAndExtractToken(fixture.outsiderCode);

        Long sharedBillId = createSharedBill(
            fixture.bookId,
            ownerToken,
            """
                {
                  "title": "权限测试共享账单",
                  "billAmountCent": 9000,
                  "categoryId": %s,
                  "payerMemberId": %s,
                  "recorderMemberId": %s,
                  "billTime": "2026-04-07T18:00:00",
                  "remark": "用于权限测试",
                  "shareItems": [
                    {
                      "participantType": "MEMBER",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 6000
                    },
                    {
                      "participantType": "TEMP_PARTICIPANT",
                      "participantRefId": %s,
                      "shareMethod": "FIXED_AMOUNT",
                      "shareAmountCent": 3000
                    }
                  ]
                }
                """.formatted(
                fixture.expenseCategoryId,
                fixture.ownerMemberId,
                fixture.ownerMemberId,
                fixture.memberMemberId,
                fixture.tempParticipantId
            ),
            "均摊账单创建成功"
        );

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills")
                .header("Authorization", "Bearer " + outsiderToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("当前用户不在该账本中"));

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId)
                .header("Authorization", "Bearer " + outsiderToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003));

        String modifyRequestResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/bills/" + sharedBillId + "/modify-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "requestReason": "我想自己先试着改",
                      "billType": "SHARED_EXPENSE",
                      "title": "权限测试共享账单-改名",
                      "billAmountCent": 9000,
                      "categoryId": %s,
                      "payerMemberId": %s,
                      "recorderMemberId": %s,
                      "billTime": "2026-04-07T18:00:00",
                      "remark": "申请修改",
                      "shareItems": [
                        {
                          "participantType": "MEMBER",
                          "participantRefId": %s,
                          "shareMethod": "FIXED_AMOUNT",
                          "shareAmountCent": 6000
                        },
                        {
                          "participantType": "TEMP_PARTICIPANT",
                          "participantRefId": %s,
                          "shareMethod": "FIXED_AMOUNT",
                          "shareAmountCent": 3000
                        }
                      ]
                    }
                    """.formatted(
                    fixture.expenseCategoryId,
                    fixture.ownerMemberId,
                    fixture.ownerMemberId,
                    fixture.memberMemberId,
                    fixture.tempParticipantId
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long requestId = extractLongField(modifyRequestResponse, "requestId");
        requestIds.add(requestId);

        mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/bill-requests/" + requestId + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "approvalComment": "我自己审批自己"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("当前用户无权审批该申请"));

        String settlementResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/settlements")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "strategyType": "INTUITIVE_FIRST"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long settlementBatchId = extractLongField(settlementResponse, "settlementBatchId");
        Long settlementTransferId = extractLongField(settlementResponse, "transferId");
        settlementBatchIds.add(settlementBatchId);
        settlementTransferIds.add(settlementTransferId);

        mockMvc.perform(get("/api/v1/books/" + fixture.bookId + "/settlements/" + settlementBatchId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("当前用户无权查看该结算批次"));

        String paymentConfirmResponse = mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/payment-confirms")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + memberToken)
                .content("""
                    {
                      "toMemberId": %s,
                      "paymentAmountCent": 3000,
                      "sourceType": "SETTLEMENT_TRANSFER",
                      "sourceRefId": %s,
                      "remark": "权限边界付款"
                    }
                    """.formatted(fixture.ownerMemberId, settlementTransferId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long paymentConfirmId = extractLongField(paymentConfirmResponse, "paymentConfirmId");
        paymentConfirmIds.add(paymentConfirmId);

        mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/payment-confirms/" + paymentConfirmId + "/confirm")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("只有收款方可以确认该付款记录"));

        mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/temp-recoveries")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content("""
                    {
                      "tempParticipantId": %s,
                      "recoveryAmountCent": 1000,
                      "remark": "无权确认"
                    }
                    """.formatted(fixture.tempParticipantId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("只有当前挂靠正式成员可以确认该临时成员回补"));

        mockMvc.perform(post("/api/v1/books/" + fixture.bookId + "/exports/personal-detail")
                .header("Authorization", "Bearer " + outsiderToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("当前用户不在该账本中"));
    }

    @Test
    void shouldCommitFailedExportStatusAndKeepSuccessfulSnapshotOnRedelivery() throws Exception {
        SharedBookFixture fixture = createSharedBookFixture("export-failure");
        ExportRecordPO record = new ExportRecordPO();
        record.setBookId(fixture.bookId);
        record.setOperatorMemberId(fixture.ownerMemberId);
        record.setExportType(com.spvermicelli.tripledger.shared.domain.enums.ExportType.PERSONAL_DETAIL);
        record.setExportStatus(com.spvermicelli.tripledger.shared.domain.enums.ExportStatus.PENDING);
        exportRecordMapper.insert(record);
        exportRecordIds.add(record.getId());
        bookMemberMapper.update(null,new LambdaUpdateWrapper<BookMemberPO>().eq(BookMemberPO::getId,fixture.ownerMemberId).set(BookMemberPO::getMemberStatus,MemberStatus.QUIT));
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,()->exportApplicationService.processExportTask(record.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(com.spvermicelli.tripledger.shared.domain.enums.ExportStatus.FAILED,exportRecordMapper.selectById(record.getId()).getExportStatus());
        bookMemberMapper.update(null,new LambdaUpdateWrapper<BookMemberPO>().eq(BookMemberPO::getId,fixture.ownerMemberId).set(BookMemberPO::getMemberStatus,MemberStatus.ACTIVE));
        exportApplicationService.processExportTask(record.getId());
        String snapshot = exportRecordMapper.selectById(record.getId()).getExportContentJson();
        org.junit.jupiter.api.Assertions.assertNotNull(snapshot);
        exportApplicationService.processExportTask(record.getId());
        org.junit.jupiter.api.Assertions.assertEquals(snapshot,exportRecordMapper.selectById(record.getId()).getExportContentJson());
    }

    private void awaitExportSnapshot(Long id) throws Exception {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            ExportRecordPO row = exportRecordMapper.selectById(id);
            if (row != null && row.getExportStatus() == com.spvermicelli.tripledger.shared.domain.enums.ExportStatus.SUCCESS) {
                org.junit.jupiter.api.Assertions.assertNotNull(row.getExportContentJson());
                org.junit.jupiter.api.Assertions.assertTrue(row.getExportContentJson().contains("billList"));
                return;
            }
            Thread.sleep(100);
        }
        org.junit.jupiter.api.Assertions.fail("Async export did not persist its snapshot within 10 seconds");
    }

    private SharedBookFixture createSharedBookFixture(String bookNamePrefix) {
        String ownerCode = randomLoginCode("bo");
        String memberCode = randomLoginCode("bm");
        String outsiderCode = randomLoginCode("bx");

        UserPO owner = insertUserForCode(ownerCode, "账本拥有者", nextMobile("138"));
        UserPO member = insertUserForCode(memberCode, "共享成员", nextMobile("139"));
        UserPO outsider = insertUserForCode(outsiderCode, "局外人", nextMobile("137"));

        BookPO book = insertBook(bookNamePrefix + "-" + UUID.randomUUID(), BookType.SHARED, owner.getId());
        BookMemberPO ownerMember = insertMember(book.getId(), owner.getId(), MemberRole.OWNER, MemberStatus.ACTIVE, owner.getId());
        BookMemberPO memberMember = insertMember(book.getId(), member.getId(), MemberRole.MEMBER, MemberStatus.ACTIVE, owner.getId());

        BookCategoryPO expenseCategory = insertCategory(
            book.getId(),
            "住宿",
            "hotel-o",
            CategoryType.EXPENSE,
            CategorySource.CUSTOM_GLOBAL,
            owner.getId()
        );
        BookCategoryPO incomeCategory = insertCategory(
            book.getId(),
            "退款",
            "refund-o",
            CategoryType.INCOME,
            CategorySource.CUSTOM_GLOBAL,
            owner.getId()
        );

        TempParticipantPO tempParticipant = insertTempParticipant(
            book.getId(),
            "儿童挂靠",
            TempParticipantType.GLOBAL,
            ownerMember.getId(),
            memberMember.getId(),
            TempParticipantStatus.ACTIVE
        );

        return new SharedBookFixture(
            ownerCode,
            memberCode,
            outsiderCode,
            book.getId(),
            owner.getId(),
            member.getId(),
            outsider.getId(),
            ownerMember.getId(),
            memberMember.getId(),
            expenseCategory.getId(),
            incomeCategory.getId(),
            tempParticipant.getId()
        );
    }

    private Long createPersonalBill(Long bookId, String token, String requestBody, String expectedMessage) throws Exception {
        String response = mockMvc.perform(post("/api/v1/books/" + bookId + "/bills/personal-bill")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value(expectedMessage))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long billId = extractLongField(response, "billId");
        billIds.add(billId);
        return billId;
    }

    private Long createSharedBill(Long bookId, String token, String requestBody, String expectedMessage) throws Exception {
        String response = mockMvc.perform(post("/api/v1/books/" + bookId + "/bills/shared-expense")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.message").value(expectedMessage))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long billId = extractLongField(response, "billId");
        billIds.add(billId);
        return billId;
    }

    private UserPO insertUserForCode(String code, String nickname, String mobile) {
        UserPO user = new UserPO();
        user.setWechatOpenId("mock-openid-" + code);
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

    private BookCategoryPO insertCategory(
        Long bookId,
        String name,
        String icon,
        CategoryType categoryType,
        CategorySource categorySource,
        Long createdByUserId
    ) {
        BookCategoryPO category = new BookCategoryPO();
        category.setBookId(bookId);
        category.setName(name);
        category.setIcon(icon);
        category.setCategoryType(categoryType);
        category.setCategorySource(categorySource);
        category.setStatus(CategoryStatus.ACTIVE);
        category.setSortOrder(1);
        category.setCreatedByUserId(createdByUserId);
        bookCategoryMapper.insert(category);
        categoryIds.add(category.getId());
        return category;
    }

    private TempParticipantPO insertTempParticipant(
        Long bookId,
        String nickname,
        TempParticipantType tempType,
        Long createdByMemberId,
        Long attachedMemberId,
        TempParticipantStatus status
    ) {
        TempParticipantPO participant = new TempParticipantPO();
        participant.setBookId(bookId);
        participant.setNickname(nickname);
        participant.setTempType(tempType);
        participant.setCreatedByMemberId(createdByMemberId);
        participant.setAttachedMemberId(attachedMemberId);
        participant.setStatus(status);
        tempParticipantMapper.insert(participant);
        tempParticipantIds.add(participant.getId());
        return participant;
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
        while (valueStart < responseBody.length() && Character.isWhitespace(responseBody.charAt(valueStart))) {
            valueStart++;
        }
        int valueEnd = valueStart;
        while (valueEnd < responseBody.length() && Character.isDigit(responseBody.charAt(valueEnd))) {
            valueEnd++;
        }
        return Long.parseLong(responseBody.substring(valueStart, valueEnd));
    }

    private String randomLoginCode(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String nextMobile(String prefix) {
        String digits = String.valueOf(Math.abs(UUID.randomUUID().hashCode()));
        String suffix = digits.length() >= 8 ? digits.substring(0, 8) : ("%08d".formatted(Integer.parseInt(digits)));
        return prefix + suffix;
    }

    private record SharedBookFixture(
        String ownerCode,
        String memberCode,
        String outsiderCode,
        Long bookId,
        Long ownerUserId,
        Long memberUserId,
        Long outsiderUserId,
        Long ownerMemberId,
        Long memberMemberId,
        Long expenseCategoryId,
        Long incomeCategoryId,
        Long tempParticipantId
    ) {
    }
}
