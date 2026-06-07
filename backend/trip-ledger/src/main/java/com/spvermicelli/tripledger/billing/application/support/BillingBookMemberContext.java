package com.spvermicelli.tripledger.billing.application.support;

import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import lombok.Builder;
import lombok.Getter;

/**
 * 账单上下文中的访问主体快照。
 * 统一封装“当前账本 + 当前成员”这组最基础的访问上下文，
 * 避免后续每个应用服务方法重复读取和返回同一组信息。
 */
@Getter
@Builder
public class BillingBookMemberContext {

    private Book book;
    private BookMember currentMember;
}
