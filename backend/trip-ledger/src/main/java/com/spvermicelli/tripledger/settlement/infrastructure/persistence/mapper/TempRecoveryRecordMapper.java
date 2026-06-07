package com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.TempRecoveryRecordPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TempRecoveryRecordMapper extends BaseMapper<TempRecoveryRecordPO> {
}
