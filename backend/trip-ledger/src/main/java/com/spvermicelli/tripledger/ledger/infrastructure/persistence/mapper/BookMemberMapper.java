package com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookMemberPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMemberMapper extends BaseMapper<BookMemberPO> {
}
