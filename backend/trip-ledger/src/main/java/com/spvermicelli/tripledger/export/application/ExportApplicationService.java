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
import com.spvermicelli.tripledger.export.infrastructure.messaging.ExportTaskPublisher;
import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookRepository;
import com.spvermicelli.tripledger.ledger.domain.category.model.BookCategory;
import com.spvermicelli.tripledger.ledger.domain.category.repository.BookCategoryRepository;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.common.response.PageResponse;
import com.spvermicelli.tripledger.shared.domain.enums.ExportStatus;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 导出应用服务。
 * 当前版本使用 RabbitMQ 将导出拆成“创建任务”和“异步生成快照”两段。
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
    private final ExportTaskPublisher exportTaskPublisher;

    public ExportApplicationService(
        BookRepository bookRepository,
        BookMemberRepository bookMemberRepository,
        ExportRecordRepository exportRecordRepository,
        BillApplicationService billApplicationService,
        StatisticsApplicationService statisticsApplicationService,
        BookCategoryRepository bookCategoryRepository,
        ObjectMapper objectMapper,
        ExportTaskPublisher exportTaskPublisher
    ) {
        this.bookRepository = bookRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.exportRecordRepository = exportRecordRepository;
        this.billApplicationService = billApplicationService;
        this.statisticsApplicationService = statisticsApplicationService;
        this.bookCategoryRepository = bookCategoryRepository;
        this.objectMapper = objectMapper;
        this.exportTaskPublisher = exportTaskPublisher;
    }

    @Transactional
    public ExportRecordResult exportPersonalDetail(Long currentUserId, Long bookId) {
        BookMember currentMember = requireActiveMember(bookId, currentUserId);
        return createExportTask(bookId, currentMember.getId(), ExportType.PERSONAL_DETAIL);
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
        return createExportTask(bookId, currentMember.getId(), ExportType.BOOK_SUMMARY);
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public void processExportTask(Long exportRecordId) {
        ExportRecord record = exportRecordRepository.findById(exportRecordId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "导出记录不存在"));
        if (record.getExportStatus() == ExportStatus.SUCCESS) {
            return;
        }

        LocalDateTime startedAt = LocalDateTime.now();
        ExportRecord runningRecord = exportRecordRepository.save(ExportRecord.builder()
            .id(record.getId())
            .bookId(record.getBookId())
            .operatorMemberId(record.getOperatorMemberId())
            .exportType(record.getExportType())
            .exportStatus(ExportStatus.RUNNING)
            .fileUrl(record.getFileUrl())
            .errorMessage(null)
            .createdAt(record.getCreatedAt())
            .startedAt(startedAt)
            .finishedAt(null)
            .build());

        try {
            BookMember operator = bookMemberRepository.findById(runningRecord.getOperatorMemberId())
                .filter(BookMember::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "导出发起成员不存在"));
            Map<String, Object> exportContent = runningRecord.getExportType() == ExportType.BOOK_SUMMARY
                ? buildBookSummaryContent(operator.getUserId(), runningRecord.getBookId())
                : buildPersonalDetailContent(operator.getUserId(), runningRecord.getBookId());
            String snapshot = toJson(exportContent);
            exportRecordRepository.save(ExportRecord.builder()
                .id(runningRecord.getId())
                .bookId(runningRecord.getBookId())
                .operatorMemberId(runningRecord.getOperatorMemberId())
                .exportType(runningRecord.getExportType())
                .exportStatus(ExportStatus.SUCCESS)
                .fileUrl(null)
                .exportContentJson(snapshot)
                .errorMessage(null)
                .createdAt(runningRecord.getCreatedAt())
                .startedAt(startedAt)
                .finishedAt(LocalDateTime.now())
                .build());
        } catch (RuntimeException exception) {
            log.error("Failed to process export task: {}", exportRecordId, exception);
            exportRecordRepository.save(ExportRecord.builder()
                .id(runningRecord.getId())
                .bookId(runningRecord.getBookId())
                .operatorMemberId(runningRecord.getOperatorMemberId())
                .exportType(runningRecord.getExportType())
                .exportStatus(ExportStatus.FAILED)
                .fileUrl(null)
                .errorMessage(exception.getMessage())
                .createdAt(runningRecord.getCreatedAt())
                .startedAt(startedAt)
                .finishedAt(LocalDateTime.now())
                .build());
            throw exception;
        }
    }

    private Map<String, Object> buildPersonalDetailContent(Long currentUserId, Long bookId) {
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
        exportContent.put("billList", allVisibleBills(currentUserId, bookId).stream()
            .map(this::toBillListItemMap)
            .toList());
        return exportContent;
    }

    private Map<String, Object> buildBookSummaryContent(Long currentUserId, Long bookId) {
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

        List<BillListItemResult> visible = allVisibleBills(currentUserId, bookId);
        visible.forEach(item -> categoryConsumption.merge(item.getCategoryId(), item.getBillAmountCent(), Long::sum));
        activeMembers.forEach(member -> memberConsumption.put(member.getId(), 0L));

        Map<String, Object> exportContent = new HashMap<>();
        exportContent.put("bookId", bookId);
        exportContent.put("bookName", book.getName());
        exportContent.put("billList", visible.stream().map(this::toBillListItemMap).toList());
        exportContent.put("exportedAt", LocalDateTime.now().toString());
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
        return exportContent;
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
                .exportStatus(record.getExportStatus().getCode())
                .fileUrl(record.getFileUrl())
                .exportContentJson(record.getExportContentJson())
                .errorMessage(record.getErrorMessage())
                .createdAt(record.getCreatedAt())
                .startedAt(record.getStartedAt())
                .finishedAt(record.getFinishedAt())
                .build())
            .toList();
    }

    private ExportRecordResult createExportTask(
        Long bookId,
        Long operatorMemberId,
        ExportType exportType
    ) {
        ExportRecord savedRecord = exportRecordRepository.save(ExportRecord.builder()
            .bookId(bookId)
            .operatorMemberId(operatorMemberId)
            .exportType(exportType)
            .exportStatus(ExportStatus.PENDING)
            .fileUrl(null)
            .errorMessage(null)
            .createdAt(LocalDateTime.now())
            .startedAt(null)
            .finishedAt(null)
            .build());
        publishExportTaskAfterCommit(savedRecord.getId());
        return ExportRecordResult.builder()
            .exportRecordId(savedRecord.getId())
            .bookId(savedRecord.getBookId())
            .operatorMemberId(savedRecord.getOperatorMemberId())
            .exportType(savedRecord.getExportType().getCode())
            .exportStatus(savedRecord.getExportStatus().getCode())
            .fileUrl(savedRecord.getFileUrl())
            .exportContentJson(null)
            .errorMessage(savedRecord.getErrorMessage())
            .createdAt(savedRecord.getCreatedAt())
            .startedAt(savedRecord.getStartedAt())
            .finishedAt(savedRecord.getFinishedAt())
            .build();
    }

    private void publishExportTaskAfterCommit(Long exportRecordId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                exportTaskPublisher.publish(exportRecordId);
            }
        });
    }

    private BookMember requireActiveMember(Long bookId, Long currentUserId) {
        return bookMemberRepository.findByBookIdAndUserId(bookId, currentUserId)
            .filter(BookMember::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "当前用户不在该账本中"));
    }

    private List<BillListItemResult> allVisibleBills(Long userId, Long bookId) {
        List<BillListItemResult> result = new java.util.ArrayList<>();
        for (int pageNo=1;;pageNo++) {
            PageResponse<BillListItemResult> page = billApplicationService.getVisibleBills(userId,bookId,pageNo,100);
            result.addAll(page.getList());
            if(result.size()>=page.getTotal() || page.getList().isEmpty()) return result;
        }
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
        map.put("billTime", result.getBillTime() == null ? null : result.getBillTime().toString());
        map.put("createdAt", result.getCreatedAt() == null ? null : result.getCreatedAt().toString());
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
