package com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestShareItemPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BillChangeRequestShareItemMapper extends BaseMapper<BillChangeRequestShareItemPO> {
}
