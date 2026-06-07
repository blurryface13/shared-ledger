package com.spvermicelli.tripledger.export.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.export.infrastructure.persistence.po.ExportRecordPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExportRecordMapper extends BaseMapper<ExportRecordPO> {
}
