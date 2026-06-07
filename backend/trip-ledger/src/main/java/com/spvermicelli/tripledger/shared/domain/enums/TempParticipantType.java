package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TempParticipantType implements BaseDbEnum {
    PRIVATE("PRIVATE"),
    GLOBAL("GLOBAL"),
    /**
     * 历史兼容值：等价于 PRIVATE。
     */
    @Deprecated
    PERSONAL("PERSONAL"),
    /**
     * 历史兼容值：等价于 PRIVATE。
     */
    @Deprecated
    PERSONAL_CARRY("PERSONAL_CARRY"),
    /**
     * 历史兼容值：等价于 GLOBAL。
     */
    @Deprecated
    SHARED_BILL("SHARED_BILL");

    @EnumValue
    @JsonValue
    private final String code;

    /**
     * 兼容历史脏数据：当数据库中的 temp_type 为空时默认按 PRIVATE 返回，
     * 防止在组装响应时出现空指针。
     */
    public static String safeCode(TempParticipantType tempParticipantType) {
        return normalize(tempParticipantType).code;
    }

    /**
     * 兼容历史入参：PERSONAL / PERSONAL_CARRY 会被映射为 PRIVATE。
     */
    public static TempParticipantType parseOrNull(String rawType) {
        if (rawType == null) {
            return null;
        }
        String normalized = rawType.trim().toUpperCase();
        return switch (normalized) {
            case "PRIVATE", "PERSONAL", "PERSONAL_CARRY" -> PRIVATE;
            case "GLOBAL", "SHARED_BILL" -> GLOBAL;
            default -> null;
        };
    }

    /**
     * 统一收敛临时成员类型，确保领域层只处理 PRIVATE / GLOBAL。
     */
    public static TempParticipantType normalize(TempParticipantType tempParticipantType) {
        if (tempParticipantType == null) {
            return PRIVATE;
        }
        return switch (tempParticipantType) {
            case PRIVATE, PERSONAL, PERSONAL_CARRY -> PRIVATE;
            case GLOBAL, SHARED_BILL -> GLOBAL;
        };
    }
}
