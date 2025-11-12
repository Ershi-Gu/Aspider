package com.ershi.aspider.datasource.domain.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据源枚举类
 *
 * @author Ershi-Gu.
 * @since 2025-11-12
 */
@Getter
public enum DataSourceTypeEnum {

    EAST_MONEY("EastMoney", "东方财富"),
    ;

    /** 数据源类型 */
    private final String type;
    /** 数据源类型中文描述 */
    private final String desc;

    DataSourceTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    /** 缓存枚举实体类 */
    private static final Map<String, DataSourceTypeEnum> CACHE;

    static {
        CACHE = Arrays.stream(DataSourceTypeEnum.values())
            .collect(Collectors.toMap(DataSourceTypeEnum::getType, Function.identity()));
    }

    /**
     * 获取枚举实例
     *
     * @param type 数据源类型
     * @return {@link DataSourceTypeEnum }
     */
    public static DataSourceTypeEnum getEnumByType(String type) {
        return CACHE.get(type);
    }

    /**
     * 获取数据源类型列表
     *
     * @return {@link List }<{@link String }>
     */
    public static List<String> getValues() {
        return new java.util.ArrayList<>(CACHE.keySet());
    }
}
