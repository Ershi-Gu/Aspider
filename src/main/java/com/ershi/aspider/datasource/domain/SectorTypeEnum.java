package com.ershi.aspider.datasource.domain;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 板块类型枚举
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
@Getter
public enum SectorTypeEnum {

    INDUSTRY("INDUSTRY", "行业板块", "m:90+t:2"),
    CONCEPT("CONCEPT", "概念板块", "m:90+t:3"),
    ;

    /** 板块类型 */
    private final String type;
    /** 中文描述 */
    private final String desc;
    /** 东财API的fs参数 */
    private final String eastMoneyFsParam;

    SectorTypeEnum(String type, String desc, String eastMoneyFsParam) {
        this.type = type;
        this.desc = desc;
        this.eastMoneyFsParam = eastMoneyFsParam;
    }

    /** 缓存枚举实体类 */
    private static final Map<String, SectorTypeEnum> CACHE;

    static {
        CACHE = Arrays.stream(SectorTypeEnum.values())
            .collect(Collectors.toMap(SectorTypeEnum::getType, Function.identity()));
    }

    /**
     * 获取枚举实例
     *
     * @param type 板块类型
     * @return {@link SectorTypeEnum }
     */
    public static SectorTypeEnum getEnumByType(String type) {
        return CACHE.get(type);
    }
}