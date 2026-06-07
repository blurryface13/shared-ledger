package com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BillChangeRequestMapper extends BaseMapper<BillChangeRequestPO> {
}
