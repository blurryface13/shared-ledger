package com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.SettlementTransferPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SettlementTransferMapper extends BaseMapper<SettlementTransferPO> {
}
