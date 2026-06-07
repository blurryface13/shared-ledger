package com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestAttachmentPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BillChangeRequestAttachmentMapper extends BaseMapper<BillChangeRequestAttachmentPO> {
}
