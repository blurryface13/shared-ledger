package com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public abstract class BaseIdPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
}
