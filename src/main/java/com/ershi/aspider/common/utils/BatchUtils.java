package com.ershi.aspider.common.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 批处理工具类
 *
 * @author Ershi-Gu.
 * @since 2025-11-14
 */
public class BatchUtils {

    /**
     * 将列表分片
     *
     * @param list      原始列表
     * @param batchSize 每片大小
     * @return 分片后的列表
     */
    public static <T> List<List<T>> partition(List<T> list, int batchSize) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("批次大小必须大于0");
        }

        List<List<T>> partitions = new ArrayList<>();
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            partitions.add(new ArrayList<>(list.subList(i, end)));
        }

        return partitions;
    }

    /**
     * 展平嵌套列表
     *
     * @param nestedList 嵌套列表
     * @return 扁平化后的列表
     */
    public static <T> List<T> flatten(List<List<T>> nestedList) {
        List<T> result = new ArrayList<>();

        for (List<T> subList : nestedList) {
            if (subList != null) {
                result.addAll(subList);
            }
        }

        return result;
    }
}
