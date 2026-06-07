package com.spvermicelli.tripledger.export.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spvermicelli.tripledger.billing.application.bill.BillApplicationService;
import com.spvermicelli.tripledger.billing.application.bill.result.BillListItemResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.AttachedTempDetailResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.CategoryConsumptionResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.MemberRelationResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.StatisticsOverviewResult;
import com.spvermicelli.tripledger.billing.application.statistics.StatisticsApplicationService;
import com.spvermicelli.tripledger.export.application.result.ExportRecordResult;
import com.spvermicelli.tripledger.export.domain.model.ExportRecord;
import com.spvermicelli.tripledger.export.domain.repository.ExportRecordRepository;
import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookRepository;
import com.spvermicelli.tripledger.ledger.domain.category.model.BookCategory;
import com.spvermicelli.tripledger.ledger.domain.category.repository.BookCategoryRepository;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.common.response.PageResponse;
import com.spvermicelli.tripledger.shared.domain.enums.ExportType;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 导出应用服务。
 * V1 当前先返回 JSON 快照和占位 fileUrl，后续接入真实文件生成器时只需替换基础设施实现。
 */
@Service
public class ExportApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ExportApplicationService.class);

    private final BookRepository bookRepository;
    private final BookMemberRepository bookMemberRepository;
    private final ExportRecordRepository exportRecordRepository;
    private final BillApplicationService billApplicationService;
    private final StatisticsApplicationService statisticsApplicationService;
    private final BookCategoryRepository bookCategoryRepository;
    private final ObjectMapper objectMapper;

    public ExportApplicationService(
        BookRepository bookRepository,
        BookMemberRepository bookMemberRepository,
        ExportRecordRepository exportRecordRepository,
        BillApplicationService billApplicationService,
        StatisticsApplicationService statisticsApplicationService,
        BookCategoryRepository bookCategoryRepository,
        ObjectMapper objectMapper
    ) {
        this.bookRepository = bookRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.exportRecordRepository = exportRecordRepository;
        this.billApplicationService = billApplicationService;
        this.statisticsApplicationService = statisticsApplicationService;
        this.bookCategoryRepository = bookCategoryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExportRecordResult exportPersonalDetail(Long currentUserId, Long bookId) {
        BookMember currentMember = requireActiveMember(bookId, currentUserId);
        Map<String, Object> exportContent = new HashMap<>();
        exportContent.put("overview", toOverviewMap(statisticsApplicationService.getOverview(currentUserId, bookId)));
        exportContent.put("categoryConsumption", statisticsApplicationService.getCategoryConsumption(currentUserId, bookId).stream()
            .map(this::toCategoryConsumptionMap)
            .toList());
        exportContent.put("memberRelations", statisticsApplicationService.getMemberRelations(currentUserId, bookId).stream()
            .map(this::toMemberRelationMap)
            .toList());
        exportContent.put("attachedTempDetails", statisticsApplicationService.getAttachedTempDetails(currentUserId, bookId).stream()
            .map(this::toAttachedTempDetailMap)
            .toList());
        exportContent.put("billList", billApplicationService.getVisibleBills(currentUserId, bookId, 1, 500).getList().stream()
            .map(this::toBillListItemMap)
            .toList());
        return saveExportRecord(bookId, currentMember.getId(), ExportType.PERSONAL_DETAIL, exportContent);
    }

    @Transactional
    public ExportRecordResult exportBookSummary(Long currentUserId, Long bookId) {
        BookMember currentMember = requireActiveMember(bookId, currentUserId);
        Book book = bookRepository.findById(bookId)
            .filter(Book::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账本不存在"));
        if (!Objects.equals(book.getOwnerUserId(), currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅账本创建者可以导出全账本消费汇总");
        }

        Map<Long, BookCategory> categoryMap = bookCategoryRepository.findVisibleForBook(bookId, currentUserId).stream()
            .collect(Collectors.toMap(BookCategory::getId, Function.identity(), (left, right) -> left));
        List<BookMember> activeMembers = bookMemberRepository.findActiveByBookId(bookId);
        Map<Long, Long> memberConsumption = new HashMap<>();
        Map<Long, Long> categoryConsumption = new HashMap<>();

        PageResponse<BillListItemResult> page = billApplicationService.getVisibleBills(currentUserId, bookId, 1, 1000);
        page.getList().forEach(item -> categoryConsumption.merge(item.getCategoryId(), item.getBillAmountCent(), Long::sum));
        activeMembers.forEach(member -> memberConsumption.put(member.getId(), 0L));

        Map<String, Object> exportContent = new HashMap<>();
        exportContent.put("bookId", bookId);
        exportContent.put("bookName", book.getName());
        exportContent.put("exportedAt", LocalDateTime.now());
        exportContent.put("categorySummary", categoryConsumption.entrySet().stream()
            .map(entry -> {
                BookCategory category = categoryMap.get(entry.getKey());
                Map<String, Object> item = new HashMap<>();
                item.put("categoryId", entry.getKey());
                item.put("categoryName", category == null ? null : category.getName());
                item.put("categoryIcon", category == null ? null : category.getIcon());
                item.put("amountCent", entry.getValue());
                return item;
            })
            .toList());
        exportContent.put("statisticsOverviewOfOwner", toOverviewMap(statisticsApplicationService.getOverview(currentUserId, bookId)));
        return saveExportRecord(bookId, currentMember.getId(), ExportType.BOOK_SUMMARY, exportContent);
    }

    @Transactional(readOnly = true)
    public List<ExportRecordResult> getExportRecords(Long currentUserId, Long bookId) {
        BookMember currentMember = requireActiveMember(bookId, currentUserId);
        return exportRecordRepository.findByBookIdAndOperatorMemberId(bookId, currentMember.getId()).stream()
            .sorted(Comparator.comparing(ExportRecord::getCreatedAt).reversed())
            .map(record -> ExportRecordResult.builder()
                .exportRecordId(record.getId())
                .bookId(record.getBookId())
                .operatorMemberId(record.getOperatorMemberId())
                .exportType(record.getExportType().getCode())
                .fileUrl(record.getFileUrl())
                .exportContentJson(null)
                .createdAt(record.getCreatedAt())
                .build())
            .toList();
    }

    private ExportRecordResult saveExportRecord(
        Long bookId,
        Long operatorMemberId,
        ExportType exportType,
        Map<String, Object> exportContent
    ) {
        ExportRecord savedRecord = exportRecordRepository.save(ExportRecord.builder()
            .bookId(bookId)
            .operatorMemberId(operatorMemberId)
            .exportType(exportType)
            .fileUrl("placeholder://exports/" + bookId + "/" + exportType.getCode().toLowerCase() + "/" + System.currentTimeMillis())
            .createdAt(LocalDateTime.now())
            .build());
        return ExportRecordResult.builder()
            .exportRecordId(savedRecord.getId())
            .bookId(savedRecord.getBookId())
            .operatorMemberId(savedRecord.getOperatorMemberId())
            .exportType(savedRecord.getExportType().getCode())
            .fileUrl(savedRecord.getFileUrl())
            .exportContentJson(toJson(exportContent))
            .createdAt(savedRecord.getCreatedAt())
            .build();
    }

    private BookMember requireActiveMember(Long bookId, Long currentUserId) {
        return bookMemberRepository.findByBookIdAndUserId(bookId, currentUserId)
            .filter(BookMember::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "当前用户不在该账本中"));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize export content", exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出内容生成失败");
        }
    }

    private Map<String, Object> toOverviewMap(StatisticsOverviewResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("payFlowAmountCent", result.getPayFlowAmountCent());
        map.put("accruedConsumptionAmountCent", result.getAccruedConsumptionAmountCent());
        map.put("receivableAmountCent", result.getReceivableAmountCent());
        map.put("pendingContributionAmountCent", result.getPendingContributionAmountCent());
        map.put("personalIncomeAmountCent", result.getPersonalIncomeAmountCent());
        map.put("recoveredFlowAmountCent", result.getRecoveredFlowAmountCent());
        map.put("settledConsumptionAmountCent", result.getSettledConsumptionAmountCent());
        return map;
    }

    private Map<String, Object> toCategoryConsumptionMap(CategoryConsumptionResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("categoryId", result.getCategoryId());
        map.put("categoryName", result.getCategoryName());
        map.put("categoryIcon", result.getCategoryIcon());
        map.put("categoryType", result.getCategoryType());
        map.put("consumptionAmountCent", result.getConsumptionAmountCent());
        return map;
    }

    private Map<String, Object> toMemberRelationMap(MemberRelationResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("targetMemberId", result.getTargetMemberId());
        map.put("targetMemberName", result.getTargetMemberName());
        map.put("targetMemberAvatarUrl", result.getTargetMemberAvatarUrl());
        map.put("iOweTargetAmountCent", result.getIOweTargetAmountCent());
        map.put("iPaidTargetAmountCent", result.getIPaidTargetAmountCent());
        map.put("iStillNeedPayTargetAmountCent", result.getIStillNeedPayTargetAmountCent());
        map.put("targetOwesMeAmountCent", result.getTargetOwesMeAmountCent());
        map.put("targetPaidMeAmountCent", result.getTargetPaidMeAmountCent());
        map.put("targetStillNeedPayMeAmountCent", result.getTargetStillNeedPayMeAmountCent());
        map.put("netDirection", result.getNetDirection());
        map.put("netAmountCent", result.getNetAmountCent());
        return map;
    }

    private Map<String, Object> toAttachedTempDetailMap(AttachedTempDetailResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("tempParticipantId", result.getTempParticipantId());
        map.put("tempParticipantNickname", result.getTempParticipantNickname());
        map.put("tempParticipantType", result.getTempParticipantType());
        map.put("totalReceivableAmountCent", result.getTotalReceivableAmountCent());
        map.put("sharedExpenseReceivableAmountCent", result.getSharedExpenseReceivableAmountCent());
        map.put("personalCarryReceivableAmountCent", result.getPersonalCarryReceivableAmountCent());
        map.put("recoveredAmountCent", result.getRecoveredAmountCent());
        map.put("unrecoveredAmountCent", result.getUnrecoveredAmountCent());
        return map;
    }

    private Map<String, Object> toBillListItemMap(BillListItemResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("billId", result.getBillId());
        map.put("billType", result.getBillType());
        map.put("title", result.getTitle());
        map.put("billAmountCent", result.getBillAmountCent());
        map.put("categoryId", result.getCategoryId());
        map.put("categoryName", result.getCategoryName());
        map.put("categoryIcon", result.getCategoryIcon());
        map.put("billTime", result.getBillTime());
        map.put("createdAt", result.getCreatedAt());
        map.put("viewerIsPayer", result.isViewerIsPayer());
        map.put("viewerIsRecorder", result.isViewerIsRecorder());
        map.put("viewerHasAttachedTempShare", result.isViewerHasAttachedTempShare());
        map.put("directEditable", result.isDirectEditable());
        map.put("directDeletable", result.isDirectDeletable());
        map.put("viewerOwnShareAmountCent", result.getViewerOwnShareAmountCent());
        map.put("viewerOwnPaidAmountCent", result.getViewerOwnPaidAmountCent());
        map.put("viewerOwnUnpaidAmountCent", result.getViewerOwnUnpaidAmountCent());
        map.put("viewerAttachedTempShareAmountCent", result.getViewerAttachedTempShareAmountCent());
        map.put("viewerAttachedTempRecoveredAmountCent", result.getViewerAttachedTempRecoveredAmountCent());
        map.put("viewerAttachedTempUnrecoveredAmountCent", result.getViewerAttachedTempUnrecoveredAmountCent());
        map.put("viewerReceivableAmountCent", result.getViewerReceivableAmountCent());
        map.put("viewerRecoveredAmountCent", result.getViewerRecoveredAmountCent());
        map.put("viewerUnrecoveredAmountCent", result.getViewerUnrecoveredAmountCent());
        return map;
    }
}
