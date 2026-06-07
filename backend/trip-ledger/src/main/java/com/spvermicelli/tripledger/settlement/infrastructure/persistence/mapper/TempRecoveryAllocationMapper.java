package com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.TempRecoveryAllocationPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TempRecoveryAllocationMapper extends BaseMapper<TempRecoveryAllocationPO> {
}
