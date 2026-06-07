package com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMapper extends BaseMapper<BookPO> {
}
