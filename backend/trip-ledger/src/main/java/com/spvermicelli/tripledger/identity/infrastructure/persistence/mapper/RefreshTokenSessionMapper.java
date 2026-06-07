package com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.RefreshTokenSessionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefreshTokenSessionMapper extends BaseMapper<RefreshTokenSessionPO> {
}
