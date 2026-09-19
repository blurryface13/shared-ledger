package com.spvermicelli.tripledger.export.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.export.infrastructure.persistence.po.ExportRecordPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExportRecordMapper extends BaseMapper<ExportRecordPO> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM tb_export_record WHERE id=#{id} FOR UPDATE")
    ExportRecordPO selectForUpdate(Long id);
}
