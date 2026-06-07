package com.spvermicelli.tripledger.billing.application.support;

import com.spvermicelli.tripledger.billing.domain.bill.model.Bill;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillShareItem;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillRepository;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillShareItemRepository;
import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookRepository;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.domain.enums.ShareParticipantType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 账单访问支撑服务。
 * 该服务集中处理：
 * 1. 当前用户是否能进入某个账本；
 * 2. 当前用户是否能查看某笔账单；
 * 3. 当前账本下批量账单的可见性过滤。
 *
 * 这样做的目标是把“可见性先行”的规则从 Controller 和具体接口用例中抽离出来，
 * 避免多个接口各自维护一份容易漂移的权限逻辑。
 */
@Component
public class BillingAccessSupportService {

    private final BookRepository bookRepository;
    private final BookMemberRepository bookMemberRepository;
    private final BillRepository billRepository;
    private final BillShareItemRepository billShareItemRepository;

    public BillingAccessSupportService(
        BookRepository bookRepository,
        BookMemberRepository bookMemberRepository,
        BillRepository billRepository,
        BillShareItemRepository billShareItemRepository
    ) {
        this.bookRepository = bookRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.billRepository = billRepository;
        this.billShareItemRepository = billShareItemRepository;
    }

    public BillingBookMemberContext requireActiveContext(Long currentUserId, Long bookId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
        if (bookId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }

        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账本不存在"));
        if (!book.isActive()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账本不存在");
        }

        BookMember currentMember = bookMemberRepository.findByBookIdAndUserId(bookId, currentUserId)
            .filter(BookMember::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "当前用户不在该账本中"));

        return BillingBookMemberContext.builder()
            .book(book)
            .currentMember(currentMember)
            .build();
    }

    public VisibleBillAggregate requireVisibleBill(Long bookId, Long billId, Long currentUserId) {
        BillingBookMemberContext context = requireActiveContext(currentUserId, bookId);
        Bill bill = billRepository.findById(billId)
            .filter(Bill::isActive)
            .filter(item -> Objects.equals(item.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账单不存在"));

        List<BillShareItem> shareItems = bill.isSharedExpense()
            ? billShareItemRepository.findByBillId(bill.getId())
            : Collections.emptyList();
        if (!canViewBill(context.getCurrentMember(), bill, shareItems)) {
            throw new BusinessException(ErrorCode.INVISIBLE, "当前用户不可查看该账单");
        }
        return VisibleBillAggregate.builder()
            .bill(bill)
            .shareItems(shareItems)
            .build();
    }

    public List<VisibleBillAggregate> listVisibleBills(Long bookId, Long currentUserId) {
        BillingBookMemberContext context = requireActiveContext(currentUserId, bookId);
        List<Bill> bills = billRepository.findActiveByBookId(bookId);
        Map<Long, List<BillShareItem>> shareItemMap = billShareItemRepository.findByBillIds(
                bills.stream()
                    .filter(Bill::isSharedExpense)
                    .map(Bill::getId)
                    .toList())
            .stream()
            .collect(Collectors.groupingBy(BillShareItem::getBillId));

        return bills.stream()
            .filter(bill -> canViewBill(context.getCurrentMember(), bill, shareItemMap.getOrDefault(bill.getId(), List.of())))
            .map(bill -> VisibleBillAggregate.builder()
                .bill(bill)
                .shareItems(shareItemMap.getOrDefault(bill.getId(), List.of()))
                .build())
            .toList();
    }

    /**
     * 核心可见性规则：
     * 1. 个人支出：仅付款人 / 记账人本人可见；
     * 2. 个人收入：仅记账人本人可见；
     * 3. 个人帮带：仅付款人 / 记账人本人可见；
     * 4. 均摊账单：付款人、正式分摊对象、临时成员挂靠正式成员可见；
     * 5. 记录人若未参与共享账单，则不因“记录人身份”获得可见权限。
     */
    public boolean canViewBill(BookMember currentMember, Bill bill, List<BillShareItem> shareItems) {
        Long currentMemberId = currentMember.getId();
        if (bill.isPersonalExpense()) {
            return Objects.equals(bill.getPayerMemberId(), currentMemberId)
                || Objects.equals(bill.getRecorderMemberId(), currentMemberId);
        }
        if (bill.isPersonalIncome()) {
            return Objects.equals(bill.getRecorderMemberId(), currentMemberId);
        }
        if (bill.isPersonalCarry()) {
            return Objects.equals(bill.getPayerMemberId(), currentMemberId)
                || Objects.equals(bill.getRecorderMemberId(), currentMemberId);
        }

        if (Objects.equals(bill.getPayerMemberId(), currentMemberId)) {
            return true;
        }

        if (shareItems == null || shareItems.isEmpty()) {
            return false;
        }

        Set<Long> visibleMemberIds = shareItems.stream()
            .filter(item -> item.getParticipantType() == ShareParticipantType.MEMBER)
            .map(BillShareItem::getParticipantRefId)
            .collect(Collectors.toSet());
        visibleMemberIds.addAll(shareItems.stream()
            .filter(item -> item.getParticipantType() == ShareParticipantType.TEMP_PARTICIPANT)
            .map(BillShareItem::getAttachedMemberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()));
        if (visibleMemberIds.contains(currentMemberId)) {
            return true;
        }

        return false;
    }
}
