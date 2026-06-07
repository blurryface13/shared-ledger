package com.spvermicelli.tripledger.shared.common.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PageResponse<T> {
    private List<T> list;
    private long pageNum;
    private long pageSize;
    private long total;
}
